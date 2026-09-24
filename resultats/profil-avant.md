# Profil de la baseline (async-profiler)

Profondeur 6, 3 coups joués. Les flamegraphs (`profiling/results/`, non suivis par git) sont générés par `./profile.sh baseline`.

## CPU (où le temps passe)

```
Self — top
    8.7 %  dames/Board.get
    6.4 %  dames/MoveGenerator.legalMoves
    4.5 %  dames/Position.plus
    3.5 %  dames/MoveGenerator.collectCaptures
    3.2 %  java/util/ArrayList.<init>
    3.1 %  dames/Move.<init>
    2.9 %  ObjArrayKlass::allocate
    2.5 %  java/util/Arrays.copyOf
    2.0 %  ObjArrayKlass::multi_allocate
    1.8 %  dames/MoveGenerator.simpleMoves
    1.7 %  dames/Piece.color
    1.7 %  dames/Board.copy

Inclusif — méthodes du projet (dames/*)
   62.8 %  dames/Bot.chooseMove
   62.7 %  dames/Bot.negamax
   47.2 %  dames/MoveGenerator.legalMoves
   14.5 %  dames/MoveGenerator.collectCaptures
   11.6 %  dames/Board.copy
    9.1 %  dames/Board.<init>
    9.0 %  dames/MoveGenerator.simpleMoves
    8.8 %  dames/Board.get
    4.9 %  dames/Position.plus
    3.1 %  dames/Move.<init>
    2.5 %  dames/Bot.evaluate
    1.7 %  dames/Piece.color

36.9 % des échantillons sont hors du code du projet (threads internes de la JVM : compilation JIT, GC...)
(1962 échantillons au total)
```

## Allocations (quels objets sont créés)

```
Self — top
   51.5 %  dames.Position
   15.0 %  java.lang.Object[]
   14.9 %  java.util.ArrayList
   10.4 %  dames.Piece[]
    2.9 %  java.util.ImmutableCollections$List12
    2.3 %  dames.Move
    0.5 %  dames.Piece[][]
    0.5 %  java.util.stream.ReferencePipeline$Head

Inclusif — méthodes du projet (dames/*)
  100.0 %  dames/Bot.chooseMove
  100.0 %  dames/Bot.negamax
   88.6 %  dames/MoveGenerator.legalMoves
   28.8 %  dames/Position.plus
   23.1 %  dames/MoveGenerator.simpleMoves
   21.0 %  dames/MoveGenerator.collectCaptures
   11.0 %  dames/Board.copy
    6.0 %  dames/Board.<init>
(5426370450 octets au total)
```

## Ramasse-miettes (`-Xlog:gc`, une exécution complète)

```
Pauses GC pendant une exécution complète (profondeur 6, 3 coups, tas 1 Go) : 10 pauses, 9.0 ms au total

[0,091s][info][gc] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 52M->1M(1024M) 1,057ms
[0,134s][info][gc] GC(1) Pause Young (Normal) (G1 Evacuation Pause) 95M->1M(1024M) 1,051ms
[0,405s][info][gc] GC(2) Pause Young (Normal) (G1 Evacuation Pause) 614M->1M(1024M) 0,928ms
[0,525s][info][gc] GC(3) Pause Young (Normal) (G1 Evacuation Pause) 614M->1M(1024M) 0,766ms
[0,647s][info][gc] GC(4) Pause Young (Normal) (G1 Evacuation Pause) 614M->1M(1024M) 0,915ms
[0,805s][info][gc] GC(5) Pause Young (Normal) (G1 Evacuation Pause) 614M->1M(1024M) 0,747ms
[0,924s][info][gc] GC(6) Pause Young (Normal) (G1 Evacuation Pause) 614M->1M(1024M) 0,761ms
[1,057s][info][gc] GC(7) Pause Young (Normal) (G1 Evacuation Pause) 614M->1M(1024M) 0,914ms
[1,179s][info][gc] GC(8) Pause Young (Normal) (G1 Evacuation Pause) 614M->1M(1024M) 1,004ms
[1,298s][info][gc] GC(9) Pause Young (Normal) (G1 Evacuation Pause) 614M->1M(1024M) 0,861ms
```
