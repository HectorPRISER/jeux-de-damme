# jeux-de-damme

Jeu de dames internationales (10x10) en Java 21, en console, avec un bot (minimax) **volontairement non optimisé** :
il sert de point de départ pour mesurer puis améliorer les performances (voir « Optimisation »).

## Jouer

- Lancer : `mvn -q exec:java` — Tests : `mvn test`
- Saisie : `b4-c5` (déplacement), `c3-e5-g7` (rafle) ; `coups` liste les coups légaux, `q` quitte.
- Contre le bot : répondre `o` à « Jouer contre le bot ? », choisir sa couleur puis la profondeur de recherche.

Règles : prise obligatoire et majoritaire, prise arrière des pions, dames volantes, promotion uniquement si le coup s'achève sur la dernière ligne.

## Les outils de mesure

Deux outils, deux questions :

| Question | Outil | Commande |
|---|---|---|
| Combien de temps ça prend ? | [hyperfine](https://github.com/sharkdp/hyperfine) | `./run_benchmarks.sh` |
| Où le temps passe-t-il ? | [async-profiler](https://github.com/async-profiler/async-profiler) (flamegraph) | `./profile.sh baseline` |

**Mise en place (une fois)** — il faut aussi Java 21 et `python3` :
```
sudo dnf install hyperfine
mkdir -p profiling && curl -sL https://github.com/async-profiler/async-profiler/releases/download/v4.5/async-profiler-4.5-linux-x64.tar.gz \
  | tar -xz -C profiling && mv profiling/async-profiler-4.5-linux-x64 profiling/async-profiler
```

### Mesurer : `./run_benchmarks.sh`

Une seule commande. Le script :
1. compile la version de départ (tag git `baseline`) et la version courante ;
2. note le matériel et la version de Java dans `results/env.txt` ;
3. vérifie que les deux versions **jouent les mêmes coups** (une optimisation ne doit pas changer le résultat) ;
4. mesure le temps des deux avec hyperfine (3 exécutions de chauffe jetées, puis 15 mesurées) ;
5. mesure les octets alloués avec [`bench/Bench.java`](bench/Bench.java) (hyperfine mesure le temps, pas la mémoire).

Résultats : `results/bench.json` (mesures brutes), `results/summary.md` (moyenne, médiane, écart-type, variance, gain)
et `results/alloc.txt` (octets alloués par appel de `legalMoves` et par recherche du bot).

Réglages possibles : `DEPTH=7 RUNS=20 ./run_benchmarks.sh` (`DEPTH`, `PLIES`, `WARMUP`, `RUNS`, `JAVA_OPTS`).

Pour que la mesure soit fiable : chargeur branché, navigateur et applis fermés, ne rien lancer pendant le run.
Si l'écart-type dépasse 5 % de la moyenne, `summary.md` le signale : refaire la mesure.

### Profiler : `./profile.sh <nom>`

Lance le bot avec async-profiler, deux fois : profil **CPU** (quelles méthodes consomment du temps) et profil
d'**allocations** (quels objets sont créés). Résultats dans `profiling/results/` :
- `<nom>-cpu.html` et `<nom>-alloc.html` : flamegraphs, à ouvrir dans le navigateur. Plus une case est large, plus elle pèse ;
  l'appelant est en bas, ce qu'il appelle est au-dessus. `Ctrl+F` cherche une méthode : elle s'affiche en magenta et
  son pourcentage apparaît en bas à droite. Le flamegraph CPU ne montre que le thread du bot : la compilation JIT de
  la JVM (≈ 1/3 des échantillons) en est retirée pour la lisibilité, mais reste comptée dans le `.txt`.
- `<nom>-cpu.txt` et `<nom>-alloc.txt` : les mêmes informations en pourcentages, faciles à recopier dans un rapport.

## Résultats de la baseline (avant optimisation)

Mesurés le 24/09/2026 sur le code non optimisé (tag `baseline`), profondeur 6, 3 coups joués.
Données brutes : [`results-avant/`](results-avant/).

### Banc d'essai
| | |
|---|---|
| CPU | Intel Core i7-1165G7 @ 2,8 GHz (jusqu'à 4,7 GHz), 4 cœurs / 8 threads |
| Caches | L1d 48 Ko et L1i 32 Ko par cœur, L2 1,25 Mo par cœur, L3 12 Mo partagé, lignes de 64 octets |
| RAM | 15 Gio |
| OS | Fedora Linux 42, noyau 6.19.14 |
| Runtime | OpenJDK 21.0.11, tas fixé à 1 Go (`-Xms1g -Xmx1g`) |
| Conditions | secteur branché, Firefox fermé, charge moyenne 0,37, gouverneur CPU `powersave` |

### Temps (hyperfine : 3 exécutions de chauffe jetées, 15 mesurées, sans shell intermédiaire)
| Version | Moyenne | Médiane | Écart-type | Variance | Min | Max |
|---|---|---|---|---|---|---|
| baseline | **1,383 s** | 1,364 s | 0,068 s (5,0 %) | 0,00468 s² | 1,307 s | 1,500 s |
| courante (code identique) | 1,445 s | 1,446 s | 0,066 s (4,6 %) | 0,00437 s² | 1,361 s | 1,549 s |

Les deux versions étant identiques, l'écart de ≈ 4 % (×0,96) est le **plancher de bruit** de la machine :
un gain inférieur à ~10 % ne serait pas crédible.

### Mémoire (`bench/Bench.java`, octets alloués exacts)
| | Alloué | Temps |
|---|---|---|
| un appel de `legalMoves` | **4 992 octets** | 0,99 µs |
| une recherche `chooseMove` (profondeur 6) | **1 971,9 Mo** | 405 ms |

Les octets sont stables d'une version à l'autre, mais le temps par appel varie de ≈ 15 % entre deux codes identiques
(0,99 µs contre 0,86 µs) : pour le temps, se fier à hyperfine. Ramasse-miettes : 10 pauses, **9 ms** au total
(`results-avant/gc.txt`).

### Où passe le temps (profil CPU, 1 962 échantillons)
![Flamegraph CPU de la baseline](docs/baseline-cpu-flamegraph.png)

| Méthode | Part du CPU total |
|---|---|
| `Bot.chooseMove` (tout le bot) | 62,8 % |
| dont `MoveGenerator.legalMoves` | **47,2 %**, soit **75 % du temps du bot** |
| dont `Board.copy` | 11,6 % (`Board.<init>` : 9,1 %, l'allocation du tableau `Piece[10][10]`) |
| hors du projet (threads de la JVM : compilation JIT, GC) | 36,9 % |

Sous `legalMoves` : `collectCaptures` 14,5 % et `simpleMoves` 9,0 %. Petits utilitaires appelés partout dans le bot :
`Board.get` 8,8 % et `Position.plus` 4,9 %.

### Allocations (profil, ≈ 5,4 Go échantillonnés)
| Type d'objet | Part des octets |
|---|---|
| `Position` | **51,5 %** |
| `Object[]` | 15,0 % |
| `ArrayList` | 14,9 % |
| `Piece[]` | 10,4 % |
| autres | 8,2 % |

88,6 % des octets sont alloués sous `legalMoves` (dont 28,8 % par `Position.plus`) et 11,0 % sous `Board.copy`.

## Optimisation

**Méthode** : un seul changement à la fois. Après chacun, lancer `./run_benchmarks.sh` : les coups sont-ils identiques ?
est-ce plus rapide ? On garde le changement s'il gagne, sinon on l'annule et on note pourquoi.

**Diagnostic de départ** (chiffres détaillés dans « Résultats de la baseline ») :
- le goulet est `MoveGenerator.legalMoves` : **75 % du temps du bot** ;
- son coût vient de la **création** de petits objets (≈ 5 Ko par appel), pas du ramasse-miettes : 10 pauses, 9 ms au total.

**Pistes** (à cocher au fur et à mesure) :

| Axe | Idée | Fait |
|---|---|---|
| Mémoire | plateau en tableau d'entiers, plus d'objets `Position`/`Piece`, jouer puis annuler le coup au lieu de copier | ☐ |
| Arrêt précoce | élagage alpha-bêta : ne pas explorer les branches qui ne peuvent plus être meilleures | ☐ |
| Concurrence | un nombre fixe de threads (= cœurs physiques, ici 4) sur les coups de départ | ☐ |
| Cache | table de transposition : ne pas recalculer une position déjà vue | ☐ |

**Journal** — une ligne par essai, y compris ceux qui échouent :

| Essai | Changement | Moyenne | Gain vs baseline | Gardé ? |
|---|---|---|---|---|
| 0 | baseline (`git tag baseline`) | 1,383 s ± 0,068 | ×1,00 | — |
