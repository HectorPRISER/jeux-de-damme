# jeux-de-damme

Jeu de dames internationales (10x10) en Java 21, en mode console.

- Lancer : `mvn -q exec:java`
- Tests : `mvn test`
- Saisie : `b4-c5` (déplacement), `c3-e5-g7` (rafle) ; `coups` liste les coups légaux, `q` quitte.
- Contre le bot : au lancement, répondre `o` à "Jouer contre le bot ?", choisir sa couleur puis la profondeur de recherche (`dames.Bot`, minimax brut sans élagage, évaluation matérielle simple — volontairement non optimisé, voir section Profilage).

Règles : prise obligatoire et majoritaire, prise arrière des pions, dames volantes, promotion uniquement si le coup s'achève sur la dernière ligne.

## Profilage CPU du craqueur (flamegraph interactif)

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
