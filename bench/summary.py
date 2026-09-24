#!/usr/bin/env python3
"""Lit le JSON produit par hyperfine et affiche un tableau statistique en Markdown.

Usage : python3 bench/summary.py results/bench.json
Le premier résultat du fichier sert de référence (baseline) pour le calcul du gain.
"""
import json
import statistics
import sys

with open(sys.argv[1]) as f:
    results = json.load(f)["results"]

print("| Version | Runs | Moyenne (s) | Médiane (s) | Écart-type (s) | Variance (s²) | Min (s) | Max (s) | Gain (moyenne) |")
print("|---|---|---|---|---|---|---|---|---|")

ref_mean = None
for r in results:
    times = r["times"]  # une durée en secondes par exécution
    mean = statistics.mean(times)
    ref_mean = ref_mean or mean
    print(
        f"| {r['command']} | {len(times)} | {mean:.3f} | {statistics.median(times):.3f} "
        f"| {statistics.stdev(times):.3f} | {statistics.variance(times):.5f} "
        f"| {min(times):.3f} | {max(times):.3f} | ×{ref_mean / mean:.2f} |"
    )

# Le coefficient de variation dit si la mesure est fiable : > 5 % = trop de bruit, refaire la mesure.
print()
for r in results:
    cv = statistics.stdev(r["times"]) / statistics.mean(r["times"]) * 100
    print(f"- {r['command']} : écart-type = {cv:.1f} % de la moyenne" + (" (⚠ bruité)" if cv > 5 else ""))
