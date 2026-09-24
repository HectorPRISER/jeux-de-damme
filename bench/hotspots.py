#!/usr/bin/env python3
"""Résume un profil « collapsed » (async-profiler) : où passe le temps ?

Usage : python3 bench/hotspots.py profiling/results/baseline-cpu.collapsed [N] [unité]

- self      : % des échantillons où la méthode est tout en haut de la pile (elle travaille elle-même).
             Pour un profil d'allocations, c'est le type d'objet créé (Position, ArrayList...).
- inclusive : % des échantillons où la méthode est quelque part dans la pile (elle + ce qu'elle appelle)
"""
import re
import sys
from collections import Counter

top_n = int(sys.argv[2]) if len(sys.argv) > 2 else 12
unit = sys.argv[3] if len(sys.argv) > 3 else "échantillons"
self_time, inclusive, total, outside_project = Counter(), Counter(), 0, 0

with open(sys.argv[1]) as f:
    for line in f:
        stack, _, count = line.rpartition(" ")
        count = int(count)
        # async-profiler ajoute un suffixe _[j] / _[i] (compilé / inliné) : on l'enlève pour lire plus facilement.
        frames = [re.sub(r"_\[.\]$", "", frame) for frame in stack.split(";")]
        total += count
        self_time[frames[-1]] += count
        if not any(frame.startswith(("dames/", "dames.")) for frame in frames):
            outside_project += count
        for frame in set(frames):
            inclusive[frame] += count


def show(title, counter, keep=lambda name: True):
    print(f"\n{title}")
    shown = 0
    for name, count in counter.most_common():
        if not keep(name):
            continue
        print(f"  {100 * count / total:5.1f} %  {name}")
        shown += 1
        if shown == top_n:
            break


show("Self — top", self_time)
show("Inclusif — méthodes du projet (dames/*)", inclusive, lambda n: n.startswith("dames/"))
if outside_project:
    print(f"\n{100 * outside_project / total:.1f} % des échantillons sont hors du code du projet "
          f"(threads internes de la JVM : compilation JIT, GC...)")
print(f"({total} {unit} au total)")
