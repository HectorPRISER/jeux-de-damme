# Performances : listes de travail réutilisées dans `legalMoves`

Les deux versions mesurées à la suite, dans la même session, à profondeur 10 (secteur branché, Firefox fermé, aucun
avertissement du script). « Avant » = `origin/main` (mémoire + alpha-bêta + cache), « Après » = `opti_listes` (+ listes
réutilisées). Le test du serveur (Vegeta) est limité à 40 requêtes/s.

## Machine

- CPU(s): 8
- Model name: 11th Gen Intel(R) Core(TM) i7-1165G7 @ 2.80GHz
- Thread(s) per core: 2
- Core(s) per socket: 4
- CPU(s) scaling MHz: 51%
- L1d cache: 192 KiB (4 instances)
- L2 cache: 5 MiB (4 instances)
- L3 cache: 12 MiB (1 instance)
- RAM 15Gi
- Fedora Linux 42 (Workstation Edition)
- openjdk version "21.0.11" 2026-04-21

## Avant (listes créées pour chaque pièce)

### Temps du bot (hyperfine, 15 mesures, profondeur 10, 3 coups joués)

| Moyenne | Médiane | Écart-type | Variance | Min | Max |
|---|---|---|---|---|---|
| 0.579 s | 0.580 s | 0.020 s | 0.00039 s² | 0.547 s | 0.606 s |

Coups joués (doivent rester identiques après optimisation) : `b4-a5 a7-b6 d4-c5`

### Mémoire (octets alloués, profondeur 10)

```
legalMoves    : 0,69 µs/appel, 3072 o/appel
chooseMove d=10: 87 ms, 392,7 Mo alloués
```

### Serveur sous charge (Vegeta, profondeur 5, 15 s par débit)

| Requêtes/s envoyées | Requêtes/s servies | Succès | p50 | p99 |
|---|---|---|---|---|
| 40 | 39.9 | 100 % | 36 ms | 49 ms |

## Après (listes créées une fois par appel de `legalMoves`)

### Temps du bot (hyperfine, 15 mesures, profondeur 10, 3 coups joués)

| Moyenne | Médiane | Écart-type | Variance | Min | Max |
|---|---|---|---|---|---|
| 0.508 s | 0.503 s | 0.017 s | 0.00030 s² | 0.485 s | 0.545 s |

Coups joués (doivent rester identiques après optimisation) : `b4-a5 a7-b6 d4-c5`

### Mémoire (octets alloués, profondeur 10)

```
legalMoves    : 0,59 µs/appel, 2008 o/appel
chooseMove d=10: 90 ms, 253,0 Mo alloués
```

### Serveur sous charge (Vegeta, profondeur 5, 15 s par débit)

| Requêtes/s envoyées | Requêtes/s servies | Succès | p50 | p99 |
|---|---|---|---|---|
| 40 | 39.9 | 100 % | 23 ms | 48 ms |

## Verdict

Temps complet 0,579 s -> 0,508 s (×1,14, soit 12 % de mieux ; écart de 10,5 erreurs standard). Mémoire par appel de
`legalMoves` 3 072 -> 2 008 octets (-35 %), par recherche 392,7 -> 253,0 Mo (-36 %). Une recherche JVM chauffée : 87 ms ->
90 ms (pas de différence mesurable). CPU total : 1,217 s -> 1,203 s. Serveur à 40 req/s : même débit servi (39,9/s), latence
médiane 36 -> 23 ms, p99 inchangé. Coups joués identiques : `b4-a5 a7-b6 d4-c5`.
