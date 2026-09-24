# Performances : avant

## Machine

- CPU(s): 8
- Model name: 11th Gen Intel(R) Core(TM) i7-1165G7 @ 2.80GHz
- Thread(s) per core: 2
- Core(s) per socket: 4
- CPU(s) scaling MHz: 42%
- L1d cache: 192 KiB (4 instances)
- L2 cache: 5 MiB (4 instances)
- L3 cache: 12 MiB (1 instance)
- RAM 15Gi
- Fedora Linux 42 (Workstation Edition)
- openjdk version "21.0.11" 2026-04-21

## Temps du bot (hyperfine, 15 mesures, profondeur 6, 3 coups joués)

| Moyenne | Médiane | Écart-type | Variance | Min | Max |
|---|---|---|---|---|---|
| 1.355 s | 1.374 s | 0.043 s | 0.00188 s² | 1.280 s | 1.421 s |

Coups joués (doivent rester identiques après optimisation) : `b4-a5 a7-b6 d4-c5`

## Mémoire (octets alloués, profondeur 6)

```
legalMoves    : 0,83 µs/appel, 4992 o/appel
chooseMove d=6: 403 ms, 1969,8 Mo alloués
```

## Serveur sous charge (Vegeta, profondeur 5, 15 s par débit)

| Requêtes/s envoyées | Requêtes/s servies | Succès | p50 | p99 |
|---|---|---|---|---|
| 10 | 10.0 | 100 % | 80 ms | 118 ms |
| 20 | 20.0 | 100 % | 75 ms | 123 ms |
| 30 | 29.8 | 100 % | 123 ms | 170 ms |
| 40 | 27.7 | 98 % | 6813 ms | 10008 ms |
