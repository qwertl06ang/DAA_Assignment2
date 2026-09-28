# DAA Assignment 2 - Data Structures

**Student:** Adilzhan Aliakbar

**Group:** SE-2521

Primitive `int` implementations of `DynamicArray`, `MyLinkedList` and `MinHeap`,
instrumented workloads W1-W4, JUnit 5 tests, and bonus B (Floyd's `buildHeap`).
The report is provided as a five-page PDF and as editable Markdown.

## Build and test

Requirements: **JDK 17 or newer** and **Maven 3.9+**. Set `JAVA_HOME` to a JDK,
and make `java` and `mvn` available on `PATH`. The first Maven build downloads
the pinned plugins and JUnit dependencies from Maven Central.

```sh
mvn clean verify
```

Expected result: **39 tests, zero failures** and `target/assignment2-1.0.jar`.
Tests compare against `ArrayList` and `PriorityQueue`, exercise empty structures,
duplicates, integer extremes and invalid indices, and check the heap property
after every insert/extraction in the test sequences.

## Run the benchmark

After building, a single command runs all cases and exports all CSV files:

```sh
java -Xms256m -Xmx1g -jar target/assignment2-1.0.jar
```

An optional first argument changes the output directory. Defaults to `results`.
The largest list workloads can take several seconds or longer on slower machines.
The already compiled JAR is also included: `java -Xms256m -Xmx1g -jar bin/assignment2-1.0.jar`.
The benchmark uses `new Random(42)`, a global warm-up of all code paths, three
discarded runs per case, and the median of **five additional measured runs**.
Setup, data generation, logging and result validation are outside timing.

W3 executes 1,000 insertions **followed by** 1,000 removals. Its middle index is
the fixed original `n / 2`, not half of the changing size. W2 has exactly 500
present values and 500 guaranteed absent negative values, shuffled deterministically.
Both sequence implementations receive identical data and queries.

## Recreate plots and report

Requirements: Python 3.10+ and the packages listed below. A virtual environment is optional.

```sh
python -m pip install -r scripts/requirements.txt
python scripts/check_results.py
python scripts/plot_results.py
python scripts/make_report.py
```

These scripts read the actual CSV results; they do not contain fabricated timings.
If you rerun the benchmark, rerun these scripts to update the plots and report.
`check_results.py` uses only Python's standard library.

## Files

| Path | Contents |
|---|---|
| `src/main/java/edu/daa/` | Three structures, shared interface, counters, benchmark |
| `src/test/java/edu/daa/` | JUnit 5 behavioral, randomized and metrics tests |
| `results/results.csv` | 36 required workload rows |
| `results/raw_samples.csv` | 180 measured samples, in nanoseconds, with checksums |
| `results/build_heap.csv` | 16 bonus construction comparisons |
| `results/build_heap_raw.csv` | 80 underlying bonus measurements |
| `results/environment.txt` | Runtime, OS, heap limit, seed, repetition policy |
| `results/plots/` | Four workload PNGs and one bonus PNG |
| `REPORT.pdf`, `REPORT.md` | Analysis, two proofs, plots and discussion |
| `DEFENSE_RU.md` | Russian explanation and preparation notes |
| `VALIDATION.md`, `results/tests.txt` | Actual execution record and test output |
| `bin/assignment2-1.0.jar` | Verified runnable build; requires Java 17+ |
| `repository.bundle` | Offline Git history included in the submission ZIP |

No collection implementation is used by production code. `Random` and `Locale`
are utility classes, not collections. Values are never stored as boxed `Integer`
in the three structures.

## Counter conventions

- `steps`: a primitive array-cell read inside an operation, or a navigation across
  a node's `next` reference, including navigation to `null`.
- `moves`: relocation of an existing array element (growth copy, shift, heap swap
  or root replacement), or an explicit structural pointer assignment to `head`,
  `tail` or `next`. A swap counts as two moves. Copying the supplied array in
  `buildHeap` counts as one read and one move per element.
- `comparisons`: one comparison of stored integer values. Index checks, loop
  bounds, pointer/null comparisons and benchmark validation are excluded.
- Placing a newly supplied value in a free array slot is not a relocation.
  Implicit zero/null initialization and local reference assignments are excluded.
  Copying a link for relinking is a move; it is not a navigation step unless the
  algorithm also advances to that node. Reading a node's `value` is not a step
  under the assignment's definition.

These are algorithm-level counters, not machine instruction or cache-miss counts.
They intentionally cover different physical operations in arrays and lists.

## Git and submission

The local repository contains `main`, `feature/array`, `feature/list`,
`feature/heap`, `feature/metrics` and release tag `v1.0`. The ZIP includes a
Git bundle so the history survives extraction without a hidden `.git` folder.

To restore the history from the ZIP:

```sh
git clone repository.bundle assignment2-repository
cd assignment2-repository
git switch main
git log --oneline --graph --all
git tag --list
```

The feature branches are visible as `origin/feature/...` after cloning the bundle.
To publish, first create an empty GitHub repository, then run the following in
the restored repository, replacing `YOUR_GITHUB_URL` with its real URL:

```sh
git remote rename origin bundle
git remote add origin YOUR_GITHUB_URL
git push origin main
git push origin refs/remotes/bundle/feature/array:refs/heads/feature/array refs/remotes/bundle/feature/list:refs/heads/feature/list refs/remotes/bundle/feature/heap:refs/heads/feature/heap refs/remotes/bundle/feature/metrics:refs/heads/feature/metrics
git push origin v1.0
```

**GitHub URL: not supplied; publication remains to be done.** Add the real URL to
your submission. The final archive is named
`DAA_Assignment2_Adilzhan_Aliakbar_SE-2521.zip`. Upload it to Moodle yourself.
