from pathlib import Path
from collections import defaultdict
import csv
from decimal import Decimal
from statistics import median

ROOT = Path(__file__).resolve().parents[1]

def check(summary_name, raw_name, expected_rows, expected_keys):
    with (ROOT / "results" / summary_name).open(newline="", encoding="utf-8") as f:
        summaries = list(csv.DictReader(f))
    with (ROOT / "results" / raw_name).open(newline="", encoding="utf-8") as f:
        raw = list(csv.DictReader(f))
    assert len(summaries) == expected_rows
    assert len(raw) == expected_rows * 5
    fields = ("workload", "variant", "structure", "n")
    groups = defaultdict(list)
    for row in raw:
        groups[tuple(row[k] for k in fields)].append(row)
    keys = {tuple(row[k] for k in fields) for row in summaries}
    assert keys == expected_keys == set(groups)
    assert len(keys) == len(summaries), "Duplicate summary rows"
    for row in summaries:
        samples = groups[tuple(row[k] for k in fields)]
        assert sorted(int(s["run"]) for s in samples) == [1, 2, 3, 4, 5]
        times = [int(s["time_ns"]) for s in samples]
        assert min(times) > 0
        assert Decimal(row["time_ms"]) == Decimal(median(times)) / 1_000_000
        for field in ("steps", "moves", "comparisons"):
            assert all(s[field] == row[field] for s in samples)
            assert int(row[field]) >= 0
        assert len({s["checksum"] for s in samples}) == 1
    print(f"PASS: {summary_name}: {expected_rows} rows, five samples each, exact medians")

if __name__ == "__main__":
    sizes = ["100", "1000", "10000", "100000"]
    expected = set()
    for n in sizes:
        for workload, variants in [("W1", ["-"]), ("W2", ["-"]), ("W3", ["head", "middle"])]:
            for variant in variants:
                for structure in ["DynamicArray", "MyLinkedList"]:
                    expected.add((workload, variant, structure, n))
        expected.add(("W4", "-", "MinHeap", n))
    check("results.csv", "raw_samples.csv", 36, expected)
    expected_build = {("Build", v, s, n) for v in ["random", "descending"]
                      for s in ["Floyd", "RepeatedInsert"] for n in sizes}
    check("build_heap.csv", "build_heap_raw.csv", 16, expected_build)
