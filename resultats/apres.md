# Performances : apres

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

## Temps du bot (hyperfine, 15 mesures, profondeur 6, 3 coups joués)

| Moyenne | Médiane | Écart-type | Variance | Min | Max |
|---|---|---|---|---|---|
| 0.137 s | 0.132 s | 0.015 s | 0.00023 s² | 0.125 s | 0.186 s |

Coups joués (doivent rester identiques après optimisation) : `b4-a5 a7-b6 d4-c5`

## Mémoire (octets alloués, profondeur 6)

```
legalMoves    : 0,80 µs/appel, 4992 o/appel
chooseMove d=6: 11 ms, 27,6 Mo alloués
```

## Serveur sous charge (Vegeta, profondeur 5, 15 s par débit)

| Requêtes/s envoyées | Requêtes/s servies | Succès | p50 | p99 |
|---|---|---|---|---|
| 10 | 10.1 | 100 % | 8 ms | 11 ms |
| 20 | 20.1 | 100 % | 8 ms | 51 ms |
| 30 | 30.0 | 100 % | 26 ms | 51 ms |
| 40 | 40.0 | 100 % | 48 ms | 52 ms |
