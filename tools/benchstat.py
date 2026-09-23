#!/usr/bin/env python3
"""Moyenne/ecart-type sur N runs de dames.Bench (profondeur 6), et delta si
une deuxieme classe (version optimisee) est donnee. Equivalent Java de
`go test -bench . -count=N` + `benchstat`.
Usage: python3 tools/benchstat.py [N] [classeA] [classeB]
  N       nombre de repetitions (defaut 7)
  classeA classe a benchmarker (defaut dames.Bench)
  classeB deuxieme classe (optionnelle) pour comparer avant/apres
"""
import re
import statistics
import subprocess
import sys

RUNS = int(sys.argv[1]) if len(sys.argv) > 1 else 7
CLASS_A = sys.argv[2] if len(sys.argv) > 2 else "dames.Bench"
CLASS_B = sys.argv[3] if len(sys.argv) > 3 else None
PATTERN = re.compile(r"profondeur 6 : coup=\S+, [^,]+ noeuds, ([\d\s ]+) ms")


def run(classname):
    times = []
    for _ in range(RUNS):
        out = subprocess.run(
            ["java", "-cp", "target/classes", classname],
            capture_output=True, text=True, check=True,
        ).stdout
        m = PATTERN.search(out)
        if not m:
            raise RuntimeError(f"Ligne 'profondeur 6' introuvable dans la sortie de {classname}:\n{out}")
        times.append(int(re.sub(r"\D", "", m.group(1))))
    return times


def report(name, times):
    mean = statistics.mean(times)
    stdev = statistics.stdev(times) if len(times) > 1 else 0.0
    pct = 100 * stdev / mean if mean else 0.0
    print(f"{name:24s} {mean:10.1f} ms ± {pct:4.1f}%   (runs: {times})")
    return mean, stdev


def main():
    print(f"benchstat (equivalent Java) - Bot.chooseMove profondeur 6, {RUNS} repetitions\n")

    a_times = run(CLASS_A)
    a_mean, a_sd = report(CLASS_A, a_times)

    if not CLASS_B:
        print("\nUn seul run fourni : mesure la stabilite de la baseline (bruit de mesure).")
        return

    b_times = run(CLASS_B)
    b_mean, b_sd = report(CLASS_B, b_times)

    delta = 100 * (b_mean - a_mean) / a_mean
    factor = a_mean / b_mean
    print(f"\ndelta: {delta:+.2f}% (x{factor:.2f} plus rapide)")
    if a_mean - a_sd > b_mean + b_sd:
        print("=> Intervalles disjoints : gain confirme, pas du bruit.")
    else:
        print("=> Intervalles qui se chevauchent : gain non concluant statistiquement.")


if __name__ == "__main__":
    main()
