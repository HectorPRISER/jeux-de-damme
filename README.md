# jeux-de-damme

Dames internationales (10x10), Java 21, console. Projet cours d'optimisation
backend : version **volontairement non optimisée** (allocations à chaque coup,
concaténations, pas d'élagage alpha-bêta) — état "avant" mesuré, à optimiser
ensuite (voir [Todolist](#todolist-optimisations) et [Tableau avant/après](#tableau-avant--après)).

## Lancer

```bash
mvn -q exec:java
```
Saisie : `b4-c5` (coup), `c3-e5-g7` (rafle) ; `coups` liste les coups légaux, `q` quitte.
Mode (1) humain [vs bot en option] ou (2) bot vs bot ([`Bot`](src/main/java/dames/Bot.java)
minimax vs [`RandomBot`](src/main/java/dames/RandomBot.java), métriques par coup).

## Front web — parties en direct

Mirror navigateur du mode bot vs bot console, via SSE (`com.sun.net.httpserver`,
zéro dépendance).

```bash
mvn -q exec:java -Dexec.mainClass=dames.Api
```
puis http://localhost:8080. Port pris (ex. Traefik) ? `-Dexec.args="9090"`.
Code : [`Api.java`](src/main/java/dames/Api.java), [`web/`](src/main/resources/web/).

## Tests

```bash
mvn test
```
Règles : prise obligatoire et majoritaire, prise arrière, dames volantes, promotion en fin de coup.

## Métriques de performance

[`dames.Bench`](src/main/java/dames/Bench.java) : temps, débit et octets alloués
(`ThreadMXBean`) sur `MoveGenerator.legalMoves` et `Bot.chooseMove`.

```bash
mvn -q -B package -DskipTests && java -cp target/classes dames.Bench
```

Baseline mesurée (JDK 21) :
```
MoveGenerator.legalMoves : 1,77 µs/appel, 4992 o/appel
Bot.chooseMove d=6       : 593 ms, 199 270 noeuds, 1972,6 Mo allouées (10380 o/noeud)
```
~11 Ko/noeud : chaque noeud copie tout le plateau (`Board.copy()`), réalloue
des `List`/`ArrayList`, recrée une `Position` par case évaluée.

Partie bot vs bot complète : `printf "2\nb\n5\n" | java -cp target/classes dames.Main`
Bout en bout (hyperfine) : `hyperfine --warmup 2 'sh -c "echo q | java -cp target/classes dames.Main"'`

### Flamegraph (async-profiler)

Setup une fois :
```bash
mkdir -p profiling && curl -sL https://github.com/async-profiler/async-profiler/releases/download/v4.5/async-profiler-4.5-linux-x64.tar.gz \
  | tar -xz -C profiling && mv profiling/async-profiler-4.5-linux-x64 profiling/async-profiler
mkdir -p profiling/results
```

```bash
# 1. profiler BotBench (profondeur 7, ignoré par mvn test, lancé explicitement)
mvn -q test -Dtest=BotBench -DargLine="-agentpath:$(pwd)/profiling/async-profiler/lib/libasyncProfiler.so=start,event=cpu,file=$(pwd)/profiling/results/cpu.jfr"

# 2. générer le flamegraph HTML
profiling/async-profiler/bin/jfrconv --cpu -o html profiling/results/cpu.jfr profiling/results/cpu-flamegraph.html

# 3. servir (JDK ≥18, pas de dépendance)
jwebserver -p 9090 -d profiling/results
```
puis http://localhost:9090/cpu-flamegraph.html.

### `tools/analyze_bottleneck.py` — quantifier le goulet

Le flamegraph montre visuellement où passe le CPU ; ce script le chiffre :
% d'échantillons dans `Board.copy()` (copie du plateau) vs `MoveGenerator`
(génération de coups) vs `Bot.evaluate`, plus le top 10 des frames feuilles.
Apporte : un chiffre exact à citer ("64,6% du CPU dans MoveGenerator") au lieu
d'un "ça a l'air gros sur le flamegraph".

```bash
python3 tools/analyze_bottleneck.py profiling/results/cpu.jfr
```

Résultat mesuré :
```
Board.copy() (copie du plateau) : 161 (25.2%)
MoveGenerator (generation)       : 413 (64.6%)
Bot.evaluate (evaluation)        : 30 (4.7%)
```
→ confirme que `MoveGenerator` (pas `Board.copy()`) est le vrai goulet, avant
même toute optimisation.

### `tools/benchstat.py` — preuve statistique (avant/après)

`Bench` donne un seul run ; le bruit machine (autre process, JIT warmup...)
peut faire croire à un gain qui n'existe pas. Ce script relance N fois,
calcule moyenne/écart-type, et — une fois une version optimisée disponible —
compare les deux avec un delta et un verdict "gain confirmé" ou "bruit".
Apporte : un gain défendable statistiquement, pas un chiffre d'un seul run.

```bash
python3 tools/benchstat.py 7                      # baseline seule (stabilité)
python3 tools/benchstat.py 7 dames.Bench dames.BenchOptimise  # une fois l'optim faite
```

### `tools/annotate_flamegraph.py` — annoter pour le rapport

Dessine un rectangle + légende sur une capture PNG du flamegraph (`google-chrome
--headless --screenshot=...`), pour pointer visuellement le goulet dans le
rapport final. Apporte : un flamegraph brut au lecteur ne dit rien sans
légende ; l'annotation ("64% MoveGenerator") rend la capture auto-porteuse.

```bash
python3 tools/annotate_flamegraph.py cpu-flamegraph.png cpu-flamegraph-annote.png \
  "0,177,1265,140,64% MoveGenerator"
```
(nécessite Pillow : `pip install pillow`)

## Todolist optimisations

- [ ] `Board.copy()` par noeud minimax → `apply`/`undo` en place
- [ ] Élagage alpha-bêta dans `Bot.negamax` (199k noeuds explorés à d=6, sans élagage)
- [ ] Allocations `MoveGenerator.legalMoves` (`ArrayList`/`List.of()` par case) → buffers réutilisables
- [ ] `new Position(r, c)` dans `Bot.evaluate` → `Board.get(int, int)`
- [ ] Concaténations `String` (`Position`/`Move`/`Board` `toString`) → buffer fixe muté par index
- [ ] `Scanner` stdin (`Main`) : impact en bot vs bot serré ?
- [ ] `Piece`/`Position`/`Move` en `record` → encodage compact (`int`/`byte`) ?
- [ ] Profondeur bot vs temps de réponse : objectif chiffré (ex. profondeur 7 < 1s)

## Tableau avant / après

| Optimisation | Métrique | Avant | Après |
|---|---|---|---|
| *(baseline)* | `Bot.chooseMove` d=6 | 593 ms, 1972,6 Mo, 10380 o/noeud | — |
| *(baseline)* | `MoveGenerator.legalMoves` | 1,77 µs/appel, 4992 o/appel | — |
| `Board.copy()` → apply/undo | o/noeud | 10380 | *TODO* |
| Élagage alpha-bêta | noeuds explorés d=6 | 199 270 | *TODO* |
| Buffers fixes `toString` | o/appel | *TODO* | *TODO* |

Reproduire une ligne : relancer `Bench` avant/après, reporter les chiffres.
Preuve statistique (N répétitions) : s'inspirer de `tools/benchstat.py` du
projet `projet-fil-rouge` voisin.
