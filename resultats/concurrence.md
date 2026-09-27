# Performances : concurrence (échec)

Les deux versions mesurées à la suite, dans la même session (machine : CPU i7-1165G7, 4 cœurs / 8 threads, 15 Gio RAM,
Fedora 42, OpenJDK 21.0.11 ; secteur branché, Firefox fermé). C'est ce même-temps qui compte : la machine dérive d'une
session à l'autre (le bot séquentiel a déjà donné 1,355 s un autre jour), donc seule une mesure côte à côte est fiable.

## Avant (séquentiel, code de `main`)

| Moyenne | Médiane | Écart-type | Variance | Min | Max |
|---|---|---|---|---|---|
| 1.514 s | 1.492 s | 0.055 s | 0.00303 s² | 1.449 s | 1.618 s |

```
legalMoves    : 0,88 µs/appel, 4992 o/appel
chooseMove d=6: 431 ms, 1971,9 Mo alloués
```

| Requêtes/s envoyées | Requêtes/s servies | Succès | p50 | p99 |
|---|---|---|---|---|
| 10 | 10.0 | 100 % | 96 ms | 116 ms |
| 20 | 19.9 | 100 % | 112 ms | 160 ms |
| 30 | 27.3 | 100 % | 1421 ms | 2819 ms |
| 40 | 20.6 | 77 % | 8454 ms | 10030 ms |

## Après (pool de 8 threads sur les coups de départ)

| Moyenne | Médiane | Écart-type | Variance | Min | Max |
|---|---|---|---|---|---|
| 1.305 s | 1.306 s | 0.072 s | 0.00513 s² | 1.205 s | 1.423 s |

```
legalMoves    : 0,97 µs/appel, 4992 o/appel
chooseMove d=6: 229 ms, 0,0 Mo alloués   (fiable seulement pour le temps : ne compte que le thread appelant, pas le pool)
```

| Requêtes/s envoyées | Requêtes/s servies | Succès | p50 | p99 |
|---|---|---|---|---|
| 10 | 10.0 | 100 % | 56 ms | 104 ms |
| 20 | 20.0 | 100 % | 63 ms | 88 ms |
| 30 | 26.0 | 100 % | 1522 ms | 6529 ms |
| 40 | 25.2 | 92 % | 3827 ms | 10012 ms |

Coups joués dans les deux cas (inchangés) : `b4-a5 a7-b6 d4-c5`.

## Verdict

Temps ×1,16, mais CPU total (user+system) ×3,5 (2,46 s -> 8,53 s) et débit servi à 30-40 req/s inchangé (≈ 26-27 req/s
dans les deux cas) : le parallélisme ne fait pas gagner de capacité, seulement de la latence à faible charge.
Analyse complète dans le README, section « Échec constructif ».
