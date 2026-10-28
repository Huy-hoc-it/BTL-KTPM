#!/usr/bin/env python3
"""Run repeatable k6 workloads and sample Linux process CPU/RSS (for Kaggle)."""
import argparse
import csv
import datetime as dt
import json
import os
import pathlib
import platform
import subprocess
import time

ROOT = pathlib.Path(__file__).resolve().parent.parent
LOAD = ROOT / "load-test"


def git_value(*args):
    result = subprocess.run(["git", *args], cwd=ROOT, text=True, capture_output=True, check=True)
    return result.stdout.strip()


def cpu_model():
    try:
        for line in pathlib.Path("/proc/cpuinfo").read_text().splitlines():
            if line.lower().startswith("model name"):
                return line.split(":", 1)[1].strip()
    except OSError:
        pass
    return platform.processor() or "unknown"


def total_memory_mib():
    try:
        for line in pathlib.Path("/proc/meminfo").read_text().splitlines():
            if line.startswith("MemTotal:"):
                return round(int(line.split()[1]) / 1024, 1)
    except OSError:
        pass
    return None


def cgroup_limits():
    cpu_quota = None
    memory_limit_mib = None
    try:
        quota, period = pathlib.Path("/sys/fs/cgroup/cpu.max").read_text().split()
        if quota != "max":
            cpu_quota = round(int(quota) / int(period), 3)
    except (OSError, ValueError, ZeroDivisionError):
        pass
    try:
        limit = pathlib.Path("/sys/fs/cgroup/memory.max").read_text().strip()
        if limit != "max":
            memory_limit_mib = round(int(limit) / (1024 * 1024), 1)
    except (OSError, ValueError):
        pass
    return cpu_quota, memory_limit_mib


def version(command):
    try:
        result = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=10)
        return result.stdout.strip().splitlines()[0]
    except (OSError, subprocess.SubprocessError, IndexError):
        return "not available"


def proc_table():
    table = {}
    for entry in pathlib.Path("/proc").iterdir():
        if not entry.name.isdigit():
            continue
        try:
            stat = (entry / "stat").read_text()
            fields = stat[stat.rfind(")") + 2:].split()
            parent = int(fields[1])
            ticks = int(fields[11]) + int(fields[12])
            rss_kib = 0
            for line in (entry / "status").read_text().splitlines():
                if line.startswith("VmRSS:"):
                    rss_kib = int(line.split()[1])
                    break
            table[int(entry.name)] = (parent, ticks, rss_kib)
        except (OSError, ValueError, IndexError):
            continue
    return table


def process_group(root_pid, table):
    if not root_pid or root_pid not in table:
        return {}
    pids = {root_pid}
    changed = True
    while changed:
        changed = False
        for pid, (parent, _, _) in table.items():
            if parent in pids and pid not in pids:
                pids.add(pid)
                changed = True
    return {pid: table[pid] for pid in pids if pid in table}


def sample_resources(k6_pid, api_pid, db_pid, writer, previous, previous_time, cpu_count):
    now = time.monotonic()
    table = proc_table()
    timestamp = dt.datetime.now(dt.timezone.utc).isoformat()
    for name, pid in (("api", api_pid), ("database", db_pid), ("k6", k6_pid)):
        group = process_group(pid, table)
        ticks = sum(values[1] for values in group.values())
        rss_kib = sum(values[2] for values in group.values())
        last_ticks = previous.get(name, ticks)
        elapsed = max(now - previous_time, 0.001)
        cpu_seconds = max(ticks - last_ticks, 0) / os.sysconf("SC_CLK_TCK")
        writer.writerow({
            "timestampUtc": timestamp,
            "group": name,
            "available": bool(group),
            "processCount": len(group),
            "cpuPercentMachine": round(cpu_seconds / elapsed / cpu_count * 100, 3),
            "summedRssMiB": round(rss_kib / 1024, 3),
        })
        previous[name] = ticks
    return now


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--scenario", choices=("movie-read", "booking", "contention"), default="booking")
    parser.add_argument("--k6", default=os.environ.get("K6", "k6"))
    parser.add_argument("--levels", type=int, nargs="+", default=(10, 25, 50))
    parser.add_argument("--repeats", type=int, default=3)
    parser.add_argument("--api-pid", type=int, default=int(os.environ.get("API_PID", "0")))
    parser.add_argument("--database-pid", type=int, default=int(os.environ.get("DATABASE_PID", "0")))
    args = parser.parse_args()
    if args.repeats < 3 or any(level < 1 or level > 50 for level in args.levels):
        parser.error("use at least three repeats and VU levels from 1 to 50")
    if args.scenario == "contention" and len(args.levels) * args.repeats > 9:
        parser.error("contention fixture has nine independent seats; use the default 10/25/50 levels and three repeats")
    if not pathlib.Path("/proc/self/stat").exists():
        parser.error("resource sampling currently requires Linux (Kaggle)")

    commit = git_value("rev-parse", "HEAD")
    dirty = git_value("status", "--porcelain", "--untracked-files=all", "--", "src/main", "pom.xml", "load-test")
    if dirty:
        parser.error("commit source and load-test scripts first so the report identifies the measured revision")

    run_root = LOAD / "results" / dt.datetime.now(dt.timezone.utc).strftime("%Y%m%d-%H%M%S")
    run_root.mkdir(parents=True, exist_ok=False)
    cpu_count = os.cpu_count() or 1
    cpu_quota, memory_limit_mib = cgroup_limits()
    metadata = {
        "collectedAtUtc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "gitCommit": commit,
        "scenario": args.scenario,
        "levels": args.levels,
        "repeats": args.repeats,
        "warmupSeconds": 30 if args.scenario in ("movie-read", "booking") else 0,
        "measureSeconds": 120 if args.scenario in ("movie-read", "booking") else "one iteration per VU",
        "cpuModel": cpu_model(),
        "logicalCpuCount": cpu_count,
        "containerCpuQuota": cpu_quota,
        "totalRamMiB": total_memory_mib(),
        "containerRamLimitMiB": memory_limit_mib,
        "os": platform.platform(),
        "python": platform.python_version(),
        "k6": version([args.k6, "version"]),
        "java": version(["java", "-version"]),
        "postgres": version(["postgres", "--version"]),
        "apiPid": args.api_pid or None,
        "databasePid": args.database_pid or None,
        "resourceNote": "summed RSS can double-count shared pages; CPU is normalized to logical CPUs",
    }
    (run_root / "environment.json").write_text(json.dumps(metadata, indent=2) + "\n")

    contention_index = 0
    for level in args.levels:
        for repeat in range(1, args.repeats + 1):
            name = f"{args.scenario}-{level}vu-run{repeat}"
            raw_path = run_root / f"{name}-raw.json"
            summary_path = run_root / f"{name}.json"
            resource_path = run_root / f"{name}-resources.csv"
            env = os.environ.copy()
            env.update({
                "VUS": str(level),
                "WARMUP_SECONDS": "30" if args.scenario in ("movie-read", "booking") else "0",
                "DURATION_SECONDS": "120" if args.scenario in ("movie-read", "booking") else "1",
                "GIT_COMMIT": commit,
                "SUMMARY_PATH": str(summary_path),
            })
            if args.scenario == "contention":
                contention_index += 1
                env["CONTENTION_RUN_INDEX"] = str(contention_index)
            command = [args.k6, "run", "--quiet", "--include-system-env-vars", "--out", f"json={raw_path}", str(LOAD / f"{args.scenario}.js")]
            with (run_root / f"{name}.log").open("w") as stdout, \
                    (run_root / f"{name}-error.log").open("w") as stderr, \
                    resource_path.open("w", newline="") as resource_file:
                writer = csv.DictWriter(resource_file, fieldnames=("timestampUtc", "group", "available", "processCount", "cpuPercentMachine", "summedRssMiB"))
                writer.writeheader()
                process = subprocess.Popen(command, cwd=ROOT, env=env, stdout=stdout, stderr=stderr)
                previous = {}
                previous_time = time.monotonic()
                while process.poll() is None:
                    previous_time = sample_resources(process.pid, args.api_pid, args.database_pid, writer,
                                                     previous, previous_time, cpu_count)
                    time.sleep(1)
                code = process.wait()
            if code:
                raise SystemExit(f"k6 failed for {name}; inspect logs in {run_root}")
            print(f"Completed {name}", flush=True)
    print(f"Results: {run_root}")


if __name__ == "__main__":
    main()
