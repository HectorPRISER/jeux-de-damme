# jeux-de-damme

Jeu de dames internationales (10x10) en Java 21, en mode console. Projet du cours
d'optimisation backend : la version actuelle est **volontairement non optimisée**
(allocations à chaque coup, concaténations, pas d'élagage alpha-bêta...) afin de
mesurer un état "avant" puis d'appliquer les optimisations une à une, avec preuve
chiffrée à chaque étape (voir [Todolist des optimisations](#todolist-des-optimisations)
et [Tableau avant / après](#tableau-avant--après)).

## Lancer le jeu

```bash
mvn -q exec:java
```

- Saisie : `b4-c5` (déplacement), `c3-e5-g7` (rafle) ; `coups` liste les coups légaux, `q` quitte.
- Au lancement, choix du mode :
  - **(1) humain** : `o` à "Jouer contre le bot ?" pour affronter [`Bot`](src/main/java/dames/Bot.java)
    (minimax brut sans élagage, évaluation matérielle simple — volontairement non
    optimisé, voir section Profilage), sinon deux joueurs humains.
  - **(2) bot vs bot** : [`Bot`](src/main/java/dames/Bot.java) (minimax, seule stratégie
    gagnante) contre [`RandomBot`](src/main/java/dames/RandomBot.java) (coups légaux au
    hasard, aucune recherche), sans saisie humaine. Après chaque coup : temps, noeuds
    explorés (côté minimax) et octets alloués ; à la fin, totaux cumulés sur la partie
    (coups, temps, Mo alloués, noeuds/s) — pratique pour comparer avant/après
    optimisation sur une partie entière plutôt qu'un seul appel isolé.

## Front web — parties en direct

Mirror dans le navigateur de ce qui s'affiche dans le terminal en mode bot vs
bot (`dames.Main`, mode 2) : même plateau, mêmes coups, mêmes métriques,
diffusés ligne par ligne via Server-Sent Events par un petit serveur HTTP JDK
natif (`com.sun.net.httpserver`, aucune dépendance ajoutée) — `Api` appelle le
même `Main.runBotVsBot`, juste redirigé vers le flux SSE au lieu de
`System.out`. Une nouvelle partie démarre automatiquement à la fin de la
précédente tant que la page reste ouverte.

```bash
mvn -q exec:java -Dexec.mainClass=dames.Api
```
puis ouvrir http://localhost:8080.

Port 8080 déjà pris sur la machine (ex. dashboard Traefik d'un autre projet) ?
Passer un autre port en argument :
```bash
mvn -q exec:java -Dexec.mainClass=dames.Api -Dexec.args="9090"
# ou, sans Maven :
mvn -q -B package -DskipTests && java -cp target/classes dames.Api 9090
```
puis ouvrir http://localhost:9090.

Code : [`Api.java`](src/main/java/dames/Api.java), [`web/`](src/main/resources/web/).

## Tests

```bash
mvn test
```

Règles : prise obligatoire et majoritaire, prise arrière des pions, dames volantes,
promotion uniquement si le coup s'achève sur la dernière ligne.

## Métriques de performance

[`dames.Bench`](src/main/java/dames/Bench.java) mesure, sans dépendance externe :

- **temps par appel** (`System.nanoTime`) et **débit** (appels/s, noeuds/s) ;
- **octets alloués par appel** via `ThreadMXBean.getThreadAllocatedBytes`
  (équivalent Java de `go test -bench . -benchmem`, sans avoir besoin d'un profileur) ;

sur les deux points chauds du projet :

1. `MoveGenerator.legalMoves` — génération des coups légaux depuis la position initiale.
2. `Bot.chooseMove` — minimax brut (profondeurs 4/5/6), avec le nombre de noeuds
   explorés (`Bot.nodesExplored()`, compteur ajouté uniquement pour l'instrumentation,
   n'affecte pas l'algorithme).

Lancer :
```bash
mvn -q -B package -DskipTests
java -cp target/classes dames.Bench
```

Résultat mesuré (baseline **avant optimisation**, JDK 21, un run) :

```
=== MoveGenerator.legalMoves (position initiale) ===
  200 000 appels : 1,77 µs/appel, 564 198 appels/s, 4992 o/appel (9 coups générés au dernier appel)

=== Bot.chooseMove (minimax brut, position initiale) ===
  profondeur 4 : coup=b4-a5, 5 013 noeuds, 79 ms, 63 860 noeuds/s, 56,1 Mo allouées (11729 o/noeud)
  profondeur 5 : coup=b4-a5, 32 130 noeuds, 145 ms, 221 303 noeuds/s, 330,7 Mo allouées (10791 o/noeud)
  profondeur 6 : coup=b4-a5, 199 270 noeuds, 593 ms, 336 261 noeuds/s, 1972,6 Mo allouées (10380 o/noeud)
```

~11 Ko alloués **par noeud** exploré : chaque noeud copie tout le plateau
(`Board.copy()`), réalloue des `List<Move>`/`ArrayList` pour chaque case, et
`evaluate()` recrée une `Position` par case parcourue. Voir todolist ci-dessous.

### Partie bot vs bot (mesure sur une partie entière)

```bash
printf "2\nb\n5\n" | java -cp target/classes dames.Main
```
(`2` = mode bot vs bot, `b` = minimax joue les blancs, `5` = profondeur). Affiche
le détail coup par coup puis le total (voir mode 2 ci-dessus).

### Benchmark de bout en bout (hyperfine)

```bash
hyperfine --warmup 2 \
  'sh -c "echo q | java -cp target/classes dames.Main"'
```

### Profilage CPU (flamegraph interactif)

Équivalent Java de `go test -cpuprofile cpu.prof` puis `go tool pprof -http=:8080`, avec [async-profiler](https://github.com/async-profiler/async-profiler).

Mise en place (une fois) :
```
mkdir -p profiling && curl -sL https://github.com/async-profiler/async-profiler/releases/download/v4.5/async-profiler-4.5-linux-x64.tar.gz \
  | tar -xz -C profiling && mv profiling/async-profiler-4.5-linux-x64 profiling/async-profiler
mkdir -p profiling/results
```

1. **Profiler le craqueur** (`dames.Bot`, minimax brut) — [`BotBench`](src/test/java/dames/BotBench.java) le fait tourner à profondeur 7 sur les premiers coups. Pas de suffixe `Test` : ignoré par `mvn test`, à lancer explicitement avec le profileur attaché à la JVM du test :
   ```
   mvn -q test -Dtest=BotBench -DargLine="-agentpath:$(pwd)/profiling/async-profiler/lib/libasyncProfiler.so=start,event=cpu,file=$(pwd)/profiling/results/cpu.jfr"
   ```
   → produit `profiling/results/cpu.jfr` (équivalent de `cpu.prof`).

2. **Générer le flamegraph interactif** à partir du profil :
   ```
   profiling/async-profiler/bin/jfrconv --cpu -o html profiling/results/cpu.jfr profiling/results/cpu-flamegraph.html
   ```

3. **Inspecter** — équivalent de `pprof -http=:8080` (le port 8080 est parfois déjà pris sur cette machine, utiliser un autre port le cas échéant) :
   ```
   jwebserver -p 8080 -d profiling/results
   ```
   puis ouvrir `http://localhost:8080/cpu-flamegraph.html` (flamegraph zoomable/cherchable, `jwebserver` est fourni avec le JDK ≥ 18, pas de dépendance en plus).

## Todolist des optimisations

État **avant** (aucune de ces optimisations n'est appliquée pour le moment). Cocher
au fur et à mesure, avec le gain mesuré (`Bench` + flamegraph) à l'appui dans le
tableau ci-dessous.

- [ ] **`Board.copy()` à chaque noeud du minimax** (`Bot.chooseMove`/`negamax`) :
      alloue un `Piece[10][10]` complet par noeud exploré (le gros du 11 Ko/noeud
      mesuré). Piste : `apply`/`undo` en place (annuler le coup après récursion)
      au lieu de copier tout le plateau.
- [ ] **Élagage alpha-bêta** dans `Bot.negamax` : minimax brut explore tous les
      noeuds (199k à profondeur 6). L'élagage coupe les branches dominées sans
      changer le résultat.
- [ ] **Allocations dans `MoveGenerator.legalMoves`** : une `List<Position>` /
      `ArrayList` neuve par case et par direction explorée, `List.of()`/
      `List.copyOf()` à chaque coup trouvé. Piste : buffers réutilisables
      (tableau de positions taille fixe, cf. buffer fixe sur pile) ou
      structures mutables réutilisées entre appels.
- [ ] **`new Position(r, c)` dans `Bot.evaluate`** : une allocation par case
      parcourue (100 par évaluation) juste pour lire `board.get(...)`. Piste :
      `Board.get(int row, int col)` sans passer par un `Position`.
- [ ] **Concaténations de `String`** : `Position.toString` (`"" + char + int`),
      `Move.toString` (`Collectors.joining`), `Board.toString` (`String.format`
      par ligne). Piste : buffer fixe (`byte[]`/`char[]`) muté par index, comme
      dans `FixedBufferBenchmark` du projet fil rouge.
- [ ] **`Scanner` pour la lecture stdin** (`Main`) : correct pour une saisie
      humaine occasionnelle, à vérifier si ça pèse dans un mode "bot vs bot"
      en boucle serrée.
- [ ] **`Piece`/`Position`/`Move` en `record`** : boxés sur le tas à chaque
      création (`Piece.promoted()`, `Position.plus()` en particulier, appelés
      dans la boucle chaude de génération de coups). Piste : voir si un
      encodage compact (`int`/`byte`) évite l'allocation sans perdre en
      lisibilité.
- [ ] **Profondeur du bot vs temps de réponse** : mesurer le point où la
      profondeur devient trop lente pour rester jouable en interactif, pour
      cadrer les optimisations sur un objectif concret (ex. "profondeur 7 en
      moins d'1s").

## Tableau avant / après

À compléter au fur et à mesure des optimisations (chiffres produits par
`dames.Bench`, une ligne par optimisation appliquée). Le "avant" ci-dessous
(baseline actuelle) est déjà mesuré ; le "après" sera rempli une fois chaque
optimisation de la todolist implémentée.

| Optimisation | Métrique | Avant | Après | Gain |
|---|---|---|---|---|
| *(baseline, aucune optimisation)* | `Bot.chooseMove` profondeur 6 | 593 ms, 336 261 noeuds/s, 1972,6 Mo allouées (10380 o/noeud) | — | — |
| *(baseline, aucune optimisation)* | `MoveGenerator.legalMoves` (position initiale) | 1,77 µs/appel, 4992 o/appel | — | — |
| `Board.copy()` → apply/undo en place | `Bot.chooseMove` profondeur 6, o/noeud | 10380 o/noeud | *TODO* | *TODO* |
| Élagage alpha-bêta | `Bot.chooseMove` profondeur 6, noeuds explorés | 199 270 noeuds | *TODO* | *TODO* |
| Buffers fixes (`Position`/`Move`/`Board` `toString`) | octets alloués / appel `toString` | *TODO (mesurer avant)* | *TODO* | *TODO* |
| ... | | | | |

Reproduire une ligne : relancer `mvn -q -B package -DskipTests && java -cp target/classes dames.Bench`
avant et après l'optimisation, reporter les chiffres. Pour une preuve statistique
(plusieurs répétitions, moyenne/écart-type comme `benchstat`), s'inspirer de
`tools/benchstat.py` du projet `projet-fil-rouge` voisin.
