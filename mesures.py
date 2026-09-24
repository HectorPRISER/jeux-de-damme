#!/usr/bin/env python3
"""Mesure les performances du bot : temps, mémoire et tenue du serveur sous charge.

    python3 mesures.py avant    # avant optimisation -> resultats/avant.md
    python3 mesures.py apres    # après optimisation -> resultats/apres.md

Conditions : chargeur branché, navigateur fermé, ne rien lancer pendant la mesure (≈ 3 minutes).
Prérequis : hyperfine et vegeta (voir README).
"""
import glob
import json
import re
import shutil
import statistics
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

DEPTH = 6                 # profondeur du bot pour le temps et la mémoire
RUNS = 15                 # exécutions mesurées par hyperfine
SERVER_DEPTH = 5          # profondeur du bot quand il répond aux requêtes du serveur
RATES = [10, 20, 30, 40]  # requêtes par seconde envoyées par Vegeta
SECONDS = 15              # durée de chaque test Vegeta
PORT = 8099

ROOT = Path(__file__).resolve().parent
CLASSES = ROOT / "target" / "mesures"
VEGETA = shutil.which("vegeta") or str(Path.home() / "go" / "bin" / "vegeta")


def sh(*cmd):
    """Lance une commande et renvoie sa sortie (le script s'arrête si la commande échoue)."""
    return subprocess.run(cmd, cwd=ROOT, check=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT).stdout.decode().strip()


def java(main_class, *args):
    return sh("java", "-cp", str(CLASSES), main_class, *map(str, args))


def machine():
    cpu = [re.sub(r"\s+", " ", line) for line in sh("lscpu").splitlines() if re.match(r"Model name|CPU\(s\)|Thread|Core|L1d|L2|L3", line)]
    ram = next(line for line in sh("free", "-h").splitlines() if line.startswith("Mem")).split()[1]
    os_name = re.search(r'PRETTY_NAME="(.*)"', Path("/etc/os-release").read_text()).group(1)
    return [*cpu, f"RAM {ram}", os_name, sh("java", "-version").splitlines()[0]]


def temps():
    """Temps d'une partie de 3 coups du bot, mesuré par hyperfine (3 exécutions de chauffe jetées)."""
    sh("hyperfine", "-N", "--warmup", "3", "--runs", str(RUNS), "--export-json", "target/hyperfine.json",
       f"java -Xms1g -Xmx1g -cp {CLASSES} BenchMain {DEPTH} 3")
    t = json.loads((ROOT / "target" / "hyperfine.json").read_text())["results"][0]["times"]
    return (f"| {statistics.mean(t):.3f} s | {statistics.median(t):.3f} s | {statistics.stdev(t):.3f} s "
            f"| {statistics.variance(t):.5f} s² | {min(t):.3f} s | {max(t):.3f} s |")


def attack(url, rate, seconds):
    """Vegeta envoie `rate` requêtes par seconde pendant `seconds` secondes ; renvoie son rapport."""
    def vegeta(*args, data):
        return subprocess.run([VEGETA, *args], input=data, stdout=subprocess.PIPE, check=True).stdout

    results = vegeta("attack", f"-rate={rate}", f"-duration={seconds}s", "-timeout=10s", data=f"GET {url}\n".encode())
    return json.loads(vegeta("report", "-type=json", data=results))


def charge():
    """Pour chaque débit : un serveur neuf, 5 s de chauffe, puis la mesure."""
    url = f"http://localhost:{PORT}/api/move?depth={SERVER_DEPTH}"
    rows = []
    for rate in RATES:
        print(f"   {rate} requêtes/s...")
        server = subprocess.Popen(["java", "-cp", str(CLASSES), "dames.Api", str(PORT)],
                                  stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        try:
            for _ in range(60):  # attend que le serveur réponde
                try:
                    urllib.request.urlopen(url, timeout=2).read()
                    break
                except OSError:
                    time.sleep(0.5)
            attack(url, 5, 5)
            r = attack(url, rate, SECONDS)
        finally:
            server.terminate()
            server.wait()
        rows.append(f"| {rate} | {r['throughput']:.1f} | {100 * r['success']:.0f} % "
                    f"| {r['latencies']['50th'] / 1e6:.0f} ms | {r['latencies']['99th'] / 1e6:.0f} ms |")
    return "\n".join(rows)


def main():
    label = sys.argv[1] if len(sys.argv) > 1 else "mesure"
    ac = glob.glob("/sys/class/power_supply/AC*/online")
    if ac and Path(ac[0]).read_text().strip() == "0":
        print("⚠ Ordinateur sur batterie : branche le chargeur.")
    if subprocess.run(["pgrep", "-x", "firefox"], stdout=subprocess.DEVNULL).returncode == 0:
        print("⚠ Firefox est ouvert : ferme-le.")

    print("Compilation...")
    shutil.rmtree(CLASSES, ignore_errors=True)
    CLASSES.mkdir(parents=True)
    sh("javac", "-d", str(CLASSES), *map(str, [*(ROOT / "src" / "main").rglob("*.java"), *(ROOT / "bench").glob("*.java")]))

    print("Temps...")
    moves = java("BenchMain", DEPTH, 3)
    time_row = temps()
    print("Mémoire...")
    memory = java("Bench", DEPTH)
    print("Charge du serveur...")
    load = charge()

    report = "\n".join([
        f"# Performances : {label}", "",
        "## Machine", "", *[f"- {line}" for line in machine()], "",
        f"## Temps du bot (hyperfine, {RUNS} mesures, profondeur {DEPTH}, 3 coups joués)", "",
        "| Moyenne | Médiane | Écart-type | Variance | Min | Max |", "|---|---|---|---|---|---|", time_row, "",
        f"Coups joués (doivent rester identiques après optimisation) : `{moves}`", "",
        f"## Mémoire (octets alloués, profondeur {DEPTH})", "", "```", memory, "```", "",
        f"## Serveur sous charge (Vegeta, profondeur {SERVER_DEPTH}, {SECONDS} s par débit)", "",
        "| Requêtes/s envoyées | Requêtes/s servies | Succès | p50 | p99 |", "|---|---|---|---|---|", load, "",
    ])
    (ROOT / "resultats").mkdir(exist_ok=True)
    (ROOT / "resultats" / f"{label}.md").write_text(report)
    print("\n" + report + f"\n-> resultats/{label}.md")


if __name__ == "__main__":
    main()
