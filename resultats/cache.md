# Performances : cache (table de transposition)

Les deux versions mesurées à la suite, dans la même session, à profondeur 10 (secteur branché, Firefox fermé, aucun
avertissement du script). « Avant » = `origin/main` (mémoire + alpha-bêta), « Après » = `opti_cache` (+ cache).
Le test du serveur (Vegeta) est limité à 40 requêtes/s, la seule valeur utile pour vérifier que le cache ne le ralentit pas.

## Machine

- CPU(s): 8
- Model name: 11th Gen Intel(R) Core(TM) i7-1165G7 @ 2.80GHz
- Thread(s) per core: 2
- Core(s) per socket: 4
- CPU(s) scaling MHz: 41%
- L1d cache: 192 KiB (4 instances)
- L2 cache: 5 MiB (4 instances)
- L3 cache: 12 MiB (1 instance)
- RAM 15Gi
- Fedora Linux 42 (Workstation Edition)
- openjdk version "21.0.11" 2026-04-21

## Avant (sans cache)

### Temps du bot (hyperfine, 15 mesures, profondeur 10, 3 coups joués)

| Moyenne | Médiane | Écart-type | Variance | Min | Max |
|---|---|---|---|---|---|
| 1.099 s | 1.090 s | 0.052 s | 0.00273 s² | 1.002 s | 1.230 s |

Coups joués (doivent rester identiques après optimisation) : `b4-a5 a7-b6 d4-c5`

### Mémoire (octets alloués, profondeur 10)

```
legalMoves    : 0,69 µs/appel, 3072 o/appel
chooseMove d=10: 282 ms, 1460,8 Mo alloués
```

### Serveur sous charge (Vegeta, profondeur 5, 15 s par débit)

| Requêtes/s envoyées | Requêtes/s servies | Succès | p50 | p99 |
|---|---|---|---|---|
| 40 | 39.9 | 100 % | 46 ms | 49 ms |

## Après (avec cache)

### Temps du bot (hyperfine, 15 mesures, profondeur 10, 3 coups joués)

| Moyenne | Médiane | Écart-type | Variance | Min | Max |
|---|---|---|---|---|---|
| 0.589 s | 0.585 s | 0.017 s | 0.00030 s² | 0.563 s | 0.618 s |

Coups joués (doivent rester identiques après optimisation) : `b4-a5 a7-b6 d4-c5`

### Mémoire (octets alloués, profondeur 10)

```
legalMoves    : 0,70 µs/appel, 3072 o/appel
chooseMove d=10: 99 ms, 392,7 Mo alloués
```

### Serveur sous charge (Vegeta, profondeur 5, 15 s par débit)

| Requêtes/s envoyées | Requêtes/s servies | Succès | p50 | p99 |
|---|---|---|---|---|
| 40 | 39.9 | 100 % | 24 ms | 49 ms |

## Verdict

Temps complet 1,099 s -> 0,589 s (×1,87), une recherche 282 ms -> 99 ms (×2,85), mémoire par recherche 1 460,8 Mo ->
392,7 Mo (÷3,7), CPU total 1,79 s -> 1,27 s. Serveur à 40 req/s : même débit servi (39,9/s), latence médiane 46 ms -> 24 ms,
p99 inchangé (49 ms). Coups joués identiques : `b4-a5 a7-b6 d4-c5`.
