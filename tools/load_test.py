#!/usr/bin/env python3
"""Test de charge du serveur SSE (dames.Api) : N clients concurrents regardent
une partie bot vs bot en direct pendant T secondes. Mesure latence de connexion,
debit d'evenements, memoire (RSS) et threads JVM sous charge — le serveur utilise
un `Executors.newCachedThreadPool()` non borne (1 thread par connexion SSE ouverte).
Usage: python3 tools/load_test.py [N clients] [T secondes] [profondeur bot] [port]
"""
import http.client
import re
import statistics
import subprocess
import sys
import threading
import time

N = int(sys.argv[1]) if len(sys.argv) > 1 else 20
DURATION = int(sys.argv[2]) if len(sys.argv) > 2 else 15
DEPTH = int(sys.argv[3]) if len(sys.argv) > 3 else 4
PORT = int(sys.argv[4]) if len(sys.argv) > 4 else 8099


def start_server():
    proc = subprocess.Popen(
        ["java", "-cp", "target/classes", "dames.Api", str(PORT)],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    for _ in range(50):
        time.sleep(0.2)
        try:
            conn = http.client.HTTPConnection("localhost", PORT, timeout=1)
            conn.request("GET", "/")
            conn.getresponse()
            conn.close()
            return proc
        except OSError:
            continue
    proc.kill()
    raise RuntimeError("le serveur n'a pas demarre a temps")


def sample_process(pid, samples, stop):
    while not stop.is_set():
        try:
            status = open(f"/proc/{pid}/status").read()
            rss = int(re.search(r"VmRSS:\s+(\d+)", status).group(1))
            threads = int(re.search(r"Threads:\s+(\d+)", status).group(1))
            samples.append((rss, threads))
        except (FileNotFoundError, AttributeError):
            pass
        time.sleep(0.5)


def client(duration, results, errors):
    start = time.time()
    try:
        conn = http.client.HTTPConnection("localhost", PORT, timeout=duration + 5)
        conn.request("GET", f"/api/stream?depth={DEPTH}")
        resp = conn.getresponse()
        connect_ms = (time.time() - start) * 1000
        lines, deadline = 0, time.time() + duration
        while time.time() < deadline:
            line = resp.fp.readline()
            if not line:
                break
            if line.startswith(b"data:"):
                lines += 1
        conn.close()
        results.append((connect_ms, lines))
    except Exception as e:
        errors.append(str(e))


def main():
    print(f"Serveur sur :{PORT}...")
    proc = start_server()
    print(f"OK (pid {proc.pid}). {N} clients concurrents, {DURATION}s, profondeur {DEPTH}...\n")

    mem_samples, stop = [], threading.Event()
    sampler = threading.Thread(target=sample_process, args=(proc.pid, mem_samples, stop), daemon=True)
    sampler.start()

    results, errors = [], []
    threads = [threading.Thread(target=client, args=(DURATION, results, errors)) for _ in range(N)]
    t0 = time.time()
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    elapsed = time.time() - t0

    stop.set()
    sampler.join()
    proc.terminate()
    proc.wait(timeout=5)

    print(f"{len(results)}/{N} clients OK, {len(errors)} erreurs, {elapsed:.1f}s reelles\n")
    if results:
        connects = [r[0] for r in results]
        lines = [r[1] for r in results]
        print(f"Latence connexion : moyenne {statistics.mean(connects):.0f} ms, max {max(connects):.0f} ms")
        print(f"Evenements/client : moyenne {statistics.mean(lines):.0f}, min {min(lines)}, max {max(lines)}")
        print(f"Debit total       : {sum(lines) / elapsed:.1f} evenements/s (tous clients)")
    if mem_samples:
        rss = [s[0] for s in mem_samples]
        thr = [s[1] for s in mem_samples]
        print(f"Memoire JVM (RSS) : depart {rss[0] / 1024:.0f} Mo -> pic {max(rss) / 1024:.0f} Mo")
        print(f"Threads JVM       : depart {thr[0]} -> pic {max(thr)}")
    if errors:
        print(f"\nErreurs (echantillon) : {errors[:3]}")


if __name__ == "__main__":
    main()
