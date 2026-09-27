# Performances : mémoire (apply/undo au lieu de copier le plateau)

Mesure officielle du 24/09/2026 avec `./run_benchmarks.sh` (avant `mesures.py`), même session, mêmes conditions
(secteur branché, Firefox fermé). « Avant » = tag `baseline` (3e94cbc), « Après » = branche `memory` (e061439).

## Machine

- CPU(s): 8
- Model name: 11th Gen Intel(R) Core(TM) i7-1165G7 @ 2.80GHz
- Thread(s) per core: 2
- Core(s) per socket: 4
- L1d cache: 48K par cœur, L2 1,3M par cœur, L3 12M partagé
- RAM 15Gi
- Fedora Linux 42 (Workstation Edition)
- openjdk version "21.0.11" 2026-04-21

## Avant (baseline, copie du plateau à chaque coup)

### Temps du bot (hyperfine, 15 mesures, profondeur 6, 3 coups joués)

| Moyenne | Médiane | Écart-type | Variance | Min | Max |
|---|---|---|---|---|---|
| 1.508 s | 1.502 s | 0.050 s | 0.00251 s² | 1.431 s | 1.648 s |

### Mémoire (octets alloués, profondeur 6)

```
legalMoves    : 0,94 µs/appel, 4992 o/appel
chooseMove d=6: 425 ms, 1971,6 Mo alloués
```

## Après (Board.apply/undo, tableau plat)

### Temps du bot (hyperfine, 15 mesures, profondeur 6, 3 coups joués)

| Moyenne | Médiane | Écart-type | Variance | Min | Max |
|---|---|---|---|---|---|
| 1.284 s | 1.283 s | 0.040 s | 0.00158 s² | 1.226 s | 1.363 s |

### Mémoire (octets alloués, profondeur 6)

```
legalMoves    : 0,73 µs/appel, 3072 o/appel
chooseMove d=6: 352 ms, 1379,3 Mo alloués
```

## Verdict

Temps complet 1,508 s → 1,284 s (×1,17), une recherche 425 ms → 352 ms (×1,21), mémoire par recherche 1 971,6 Mo →
1 379,3 Mo (−30 %), mémoire par appel de `legalMoves` 4 992 o → 3 072 o (−38 %, soit 80 objets `Position` de 24 octets
en moins). Coups joués identiques : `b4-a5 a7-b6 d4-c5`, vérifié sur 9 000 positions contre le bot d'origine ; d'après
Hector, le nombre de nœuds explorés est aussi identique (199 270).

Le gain est celui que le profil de la baseline prévoyait : `Board.copy` pesait 11,6 % du CPU, donc le supprimer ne
pouvait pas faire gagner plus de ×1,13 environ ; on mesure ×1,17, le tableau plat et les `Position` évitées aidant un
peu de plus.
