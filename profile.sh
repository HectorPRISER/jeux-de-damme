#!/usr/bin/env bash
# Profil CPU + allocations du bot avec async-profiler (équivalent Java de pprof).
#
#   ./profile.sh baseline     # à lancer AVANT d'optimiser
#   ./profile.sh optimise     # à relancer APRÈS, pour comparer les flamegraphs
#
# Produit dans profiling/results/ :  <nom>-cpu.html, <nom>-alloc.html (flamegraphs à ouvrir dans Firefox)
# et <nom>-cpu.txt, <nom>-alloc.txt (résumés chiffrés, à recopier dans le rapport).
set -euo pipefail
cd "$(dirname "$0")"

LABEL=${1:-current}
DEPTH=${DEPTH:-6}
PLIES=${PLIES:-3}
AP=profiling/async-profiler
RES=profiling/results

[ -f "$AP/lib/libasyncProfiler.so" ] || { echo "async-profiler absent : voir la section « Mise en place » du README"; exit 1; }
mkdir -p "$RES" target/profile
javac -d target/profile $(find src/main -name '*.java') bench/BenchMain.java

# DebugNonSafepoints : demande à la JVM des positions de code plus précises pour le profileur.
JVM="-Xms1g -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints"

echo "==> Profil CPU (où le processeur passe son temps)"
java $JVM -agentpath:"$PWD/$AP/lib/libasyncProfiler.so=start,event=cpu,interval=1ms,file=$RES/$LABEL-cpu.jfr" \
    -cp target/profile BenchMain "$DEPTH" "$PLIES" >/dev/null
# Le flamegraph ne montre que le thread du bot (-I) et masque les cases minuscules (--minwidth) : sinon la
# compilation JIT de la JVM (~1/3 des échantillons) rend le graphe très haut et illisible.
# Le résumé chiffré (.collapsed -> .txt) garde, lui, tous les échantillons.
"$AP/bin/jfrconv" --cpu --minwidth 0.2 -I '.*BenchMain.*' -o html "$RES/$LABEL-cpu.jfr" "$RES/$LABEL-cpu.html"
"$AP/bin/jfrconv" --cpu -o collapsed "$RES/$LABEL-cpu.jfr" "$RES/$LABEL-cpu.collapsed"
python3 bench/hotspots.py "$RES/$LABEL-cpu.collapsed" | tee "$RES/$LABEL-cpu.txt"

echo
echo "==> Profil d'allocations (quels objets sont créés sur le tas, et par quelles méthodes)"
java $JVM -agentpath:"$PWD/$AP/lib/libasyncProfiler.so=start,event=alloc,file=$RES/$LABEL-alloc.jfr" \
    -cp target/profile BenchMain "$DEPTH" "$PLIES" >/dev/null
"$AP/bin/jfrconv" --alloc --total --minwidth 0.2 -o html "$RES/$LABEL-alloc.jfr" "$RES/$LABEL-alloc.html"
"$AP/bin/jfrconv" --alloc --total -o collapsed "$RES/$LABEL-alloc.jfr" "$RES/$LABEL-alloc.collapsed"
python3 bench/hotspots.py "$RES/$LABEL-alloc.collapsed" 8 octets | tee "$RES/$LABEL-alloc.txt"

echo
echo "Flamegraphs : firefox $RES/$LABEL-cpu.html $RES/$LABEL-alloc.html"
