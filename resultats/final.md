# Performances : synthèse finale, baseline contre version finale

Les deux versions mesurées à la suite, dans la même session, avec le protocole standard (`python3 mesures.py` :
profondeur 6, 3 coups, débits de 10 à 40 requêtes/s), secteur branché, Firefox fermé, aucun avertissement du script.
« Baseline » = le code de départ avec les mêmes outils de mesure ; « Finale » = `main` avec la mémoire, l'alpha-bêta, le
cache et les listes réutilisées (sans la concurrence, en échec).

## Machine

- CPU(s): 8
- Model name: 11th Gen Intel(R) Core(TM) i7-1165G7 @ 2.80GHz
- Thread(s) per core: 2
- Core(s) per socket: 4
- CPU(s) scaling MHz: 33%
- L1d cache: 192 KiB (4 instances)
- L2 cache: 5 MiB (4 instances)
- L3 cache: 12 MiB (1 instance)
- RAM 15Gi
- Fedora Linux 42 (Workstation Edition)
- openjdk version "21.0.11" 2026-04-21

## Baseline

### Temps du bot (hyperfine, 15 mesures, profondeur 6, 3 coups joués)

| Moyenne | Médiane | Écart-type | Variance | Min | Max |
|---|---|---|---|---|---|
| 1.317 s | 1.316 s | 0.042 s | 0.00177 s² | 1.262 s | 1.395 s |

Coups joués (doivent rester identiques après optimisation) : `b4-a5 a7-b6 d4-c5`

### Mémoire (octets alloués, profondeur 6)

```
legalMoves    : 0,79 µs/appel, 4992 o/appel
chooseMove d=6: 403 ms, 1969,8 Mo alloués
```

### Serveur sous charge (Vegeta, profondeur 5, 15 s par débit)

| Requêtes/s envoyées | Requêtes/s servies | Succès | p50 | p99 |
|---|---|---|---|---|
| 10 | 10.0 | 100 % | 98 ms | 111 ms |
| 20 | 19.9 | 100 % | 64 ms | 115 ms |
| 30 | 29.9 | 100 % | 113 ms | 155 ms |
| 40 | 26.7 | 95 % | 7088 ms | 10010 ms |

## Version finale

### Temps du bot (hyperfine, 15 mesures, profondeur 6, 3 coups joués)

| Moyenne | Médiane | Écart-type | Variance | Min | Max |
|---|---|---|---|---|---|
| 0.108 s | 0.105 s | 0.006 s | 0.00003 s² | 0.101 s | 0.118 s |

Coups joués (doivent rester identiques après optimisation) : `b4-a5 a7-b6 d4-c5`

### Mémoire (octets alloués, profondeur 6)

```
legalMoves    : 0,59 µs/appel, 2008 o/appel
chooseMove d=6: 9 ms, 6,9 Mo alloués
```

### Serveur sous charge (Vegeta, profondeur 5, 15 s par débit)

| Requêtes/s envoyées | Requêtes/s servies | Succès | p50 | p99 |
|---|---|---|---|---|
| 10 | 10.1 | 100 % | 5 ms | 8 ms |
| 20 | 20.1 | 100 % | 4 ms | 8 ms |
| 30 | 30.0 | 100 % | 23 ms | 48 ms |
| 40 | 39.9 | 100 % | 23 ms | 48 ms |

## Ordre de grandeur à profondeur 8 (temps complet, 3 coups)

| Version | Mesures | Temps |
|---|---|---|
| Baseline | 1 seule (elle dure ≈ 38 s) | 37.8 s |
| Finale | 10 | 0.174 s ± 0.012 |

Gain : ×217. La baseline n'a été mesurée qu'une fois : l'écart est tel que le bruit n'y change rien.

## Verdict

Temps complet à profondeur 6 : 1,317 s -> 0,108 s (×12,2). À profondeur 8 : 37,8 s -> 0,174 s (×217). Mémoire par recherche à
profondeur 6 : 1 969,8 Mo -> 6,9 Mo (÷285). Serveur à 40 req/s : 26,7/s servies (95 %) -> 39,9/s (100 %), latence médiane
7 088 ms -> 23 ms. Coups joués identiques dans les deux versions : `b4-a5 a7-b6 d4-c5`.
