#!/usr/bin/env python3
"""Quantifie la part du CPU dans Board.copy() (allocation/copie) vs le reste
du minimax (generation de coups, evaluation), a partir d'un profil JFR.
Usage: python3 tools/analyze_bottleneck.py profiling/results/cpu.jfr
"""
import subprocess
import sys
from collections import Counter

COPY_MARKERS = ("dames.Board.copy", "Piece[].clone", "clone")
GENERATOR_MARKERS = ("dames.MoveGenerator",)
EVAL_MARKERS = ("dames.Bot.evaluate",)


def parse_stacks(jfr_path):
    proc = subprocess.run(
        ["jfr", "print", "--events", "jdk.ExecutionSample", "--stack-depth", "128", jfr_path],
        capture_output=True, text=True, check=True,
    )
    stacks, current, in_stack = [], [], False
    for line in proc.stdout.splitlines():
        stripped = line.strip()
        if stripped == "stackTrace = [":
            in_stack, current = True, []
            continue
        if in_stack:
            if stripped == "]":
                in_stack = False
                if current:
                    stacks.append(current)
                continue
            current.append(stripped.rsplit(" line:", 1)[0])
    return stacks


def pct(stacks, markers):
    hit = sum(1 for s in stacks if any(any(m in f for m in markers) for f in s))
    return hit, 100 * hit / len(stacks) if stacks else 0.0


def main():
    if len(sys.argv) != 2:
        print("Usage: analyze_bottleneck.py <fichier.jfr>", file=sys.stderr)
        sys.exit(1)

    stacks = parse_stacks(sys.argv[1])
    total = len(stacks)
    if total == 0:
        print("Aucun echantillon jdk.ExecutionSample trouve.", file=sys.stderr)
        sys.exit(1)

    copy_n, copy_pct = pct(stacks, COPY_MARKERS)
    gen_n, gen_pct = pct(stacks, GENERATOR_MARKERS)
    eval_n, eval_pct = pct(stacks, EVAL_MARKERS)

    print(f"Echantillons totaux             : {total}")
    print(f"Board.copy() (copie du plateau) : {copy_n} ({copy_pct:.1f}%)")
    print(f"MoveGenerator (generation)       : {gen_n} ({gen_pct:.1f}%)")
    print(f"Bot.evaluate (evaluation)        : {eval_n} ({eval_pct:.1f}%)")

    print("\nTop frames feuilles (ou le temps CPU est reellement passe) :")
    for name, count in Counter(s[0] for s in stacks if s).most_common(10):
        print(f"  {100 * count / total:5.1f}%  {count:5d}  {name}")


if __name__ == "__main__":
    main()
