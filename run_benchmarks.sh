#!/usr/bin/env bash
# Benchmark « avant / après » en UNE commande :   ./run_benchmarks.sh
#
#   1. compile la version de départ (tag git "baseline") et la version courante (ton dossier de travail)
#   2. note l'environnement (CPU, caches, RAM, OS, Java...) dans results/env.txt
#   3. vérifie que les deux versions jouent les mêmes coups
#   4. mesure les deux avec hyperfine et écrit results/bench.json, bench.md et summary.md
#   5. mesure les octets alloués (bench/Bench.java) et écrit results/alloc.txt
#
# Réglages possibles :  DEPTH=7 RUNS=20 ./run_benchmarks.sh
set -euo pipefail
cd "$(dirname "$0")"

BASELINE_REF=${BASELINE_REF:-baseline}   # commit/tag de la version non optimisée
DEPTH=${DEPTH:-6}                         # profondeur de recherche du bot
PLIES=${PLIES:-3}                         # nombre de demi-coups joués par le bot
WARMUP=${WARMUP:-3}                       # exécutions jetées avant de mesurer (cache disque, fréquence CPU)
RUNS=${RUNS:-15}                          # exécutions mesurées
JAVA_OPTS=${JAVA_OPTS:--Xms1g -Xmx1g}     # tas fixe : évite le bruit dû au redimensionnement du tas
OUT=${OUT:-results}
WORK=target/bench

command -v hyperfine >/dev/null || { echo "hyperfine introuvable : sudo dnf install hyperfine"; exit 1; }
git rev-parse --verify -q "$BASELINE_REF^{commit}" >/dev/null ||
    { echo "Réf git '$BASELINE_REF' introuvable. Crée-la sur le commit non optimisé : git tag baseline <commit>"; exit 1; }

AC=$(cat /sys/class/power_supply/AC*/online 2>/dev/null | head -1 || true)   # 1 = secteur, 0 = batterie

mkdir -p "$OUT"
rm -rf "$WORK"
mkdir -p "$WORK/baseline-src" "$WORK/baseline" "$WORK/current"

echo "==> 1/5 Compilation"
git archive "$BASELINE_REF" src/main | tar -x -C "$WORK/baseline-src"
javac -d "$WORK/baseline" $(find "$WORK/baseline-src/src/main" -name '*.java') bench/*.java
javac -d "$WORK/current" $(find src/main -name '*.java') bench/*.java

echo "==> 2/5 Environnement -> $OUT/env.txt"
{
    echo "date            : $(date -Is)"
    echo "git             : $(git rev-parse --short HEAD) sur '$(git branch --show-current)' (baseline = $(git rev-parse --short "$BASELINE_REF"))$(git status --porcelain src | grep -q . && echo ' + modifications de src/ non commitées' || true)"
    echo "--- CPU"
    lscpu | grep -E 'Model name|^CPU\(s\)|Thread|Core|Socket|CPU max MHz'
    echo "--- Caches (L1d/L1i par coeur, L2 par coeur, L3 partagé)"
    lscpu -C 2>/dev/null || lscpu | grep -E 'L1|L2|L3'
    echo "--- RAM"
    free -h | head -2
    echo "--- OS"
    grep PRETTY_NAME /etc/os-release
    uname -r
    echo "--- Runtime"
    java -version 2>&1
    hyperfine --version
    echo "--- État de la machine pendant la mesure"
    echo "gouverneur CPU  : $(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || echo inconnu)"
    echo "secteur branché : ${AC:-inconnu} (1 = oui, 0 = sur batterie)"
    echo "charge moyenne  : $(uptime | sed 's/.*load average: //')"
    echo "--- Paramètres du benchmark"
    echo "profondeur=$DEPTH demi-coups=$PLIES warmup=$WARMUP runs=$RUNS JAVA_OPTS='$JAVA_OPTS'"
} | tee "$OUT/env.txt" >/dev/null

[ "$AC" = "0" ] && echo "⚠  Ordinateur sur batterie : branche le chargeur, sinon les mesures seront bruitées."

echo "==> 3/5 Vérification : les deux versions jouent-elles les mêmes coups ?"
base_moves=$(java $JAVA_OPTS -cp "$WORK/baseline" BenchMain "$DEPTH" "$PLIES")
cur_moves=$(java $JAVA_OPTS -cp "$WORK/current" BenchMain "$DEPTH" "$PLIES")
if [ "$base_moves" != "$cur_moves" ]; then
    echo "✗ Les coups diffèrent : l'optimisation a changé le comportement du bot !"
    echo "  baseline : $base_moves"
    echo "  courante : $cur_moves"
    exit 1
fi
echo "✓ mêmes coups : $base_moves"

echo "==> 4/5 Mesure du temps (hyperfine, $WARMUP warmups + $RUNS runs par version)"
# -N : pas de shell intermédiaire (moins de bruit) ; la sortie du programme est ignorée.
hyperfine -N --warmup "$WARMUP" --runs "$RUNS" \
    --export-json "$OUT/bench.json" --export-markdown "$OUT/bench.md" \
    -n baseline "java $JAVA_OPTS -cp $WORK/baseline BenchMain $DEPTH $PLIES" \
    -n current "java $JAVA_OPTS -cp $WORK/current BenchMain $DEPTH $PLIES"

echo
python3 bench/summary.py "$OUT/bench.json" | tee "$OUT/summary.md"

echo
echo "==> 5/5 Mémoire (octets alloués, profondeur $DEPTH)"
for version in baseline current; do
    echo "--- $version"
    java $JAVA_OPTS -cp "$WORK/$version" Bench "$DEPTH"
done | tee "$OUT/alloc.txt"
