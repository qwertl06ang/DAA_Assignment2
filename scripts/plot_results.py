from pathlib import Path
import csv
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter, LogLocator, NullFormatter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "results" / "plots"
COLORS = {"DynamicArray": "#166B9A", "MyLinkedList": "#C56B27", "MinHeap": "#23846C",
          "RepeatedInsert": "#166B9A", "Floyd": "#8B4A9D"}

def read(name):
    with (ROOT / "results" / name).open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))

def axes_style(ax, field, all_zero=False):
    ax.set_xscale("log")
    ax.set_xticks([100, 1000, 10000, 100000], ["100", "1k", "10k", "100k"])
    ax.set_xlabel("Initial size n")
    if field == "time_ms":
        ax.set_yscale("log")
        ax.set_ylabel("Median time (ms)")
    else:
        ax.set_yscale("linear" if all_zero else "log")
        ax.set_ylabel(f"{field.capitalize()} (count)")
        if not all_zero:
            ax.yaxis.set_major_locator(LogLocator(base=10, numticks=5))
        ax.yaxis.set_major_formatter(FuncFormatter(lambda v, _: f"{v:g}" if v < 1000 else f"{v:.0e}"))
    ax.yaxis.set_minor_formatter(NullFormatter())
    ax.xaxis.set_minor_formatter(NullFormatter())
    ax.grid(True, which="major", color="#d6dfe5", linewidth=.55)
    for spine in ["top", "right"]:
        ax.spines[spine].set_visible(False)
    ax.legend(fontsize=6.7, loc="best", framealpha=.94)

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 8,
                         "axes.titlesize": 9, "axes.labelsize": 8,
                         "savefig.facecolor": "white", "figure.facecolor": "white"})
    data = read("results.csv")
    titles = {"W1": "W1 | Random access: 10,000 get calls", "W2": "W2 | Search: 500 hits + 500 misses",
              "W3": "W3 | 1,000 insertions, then 1,000 removals", "W4": "W4 | Insert n, then extract n minima"}
    for workload, title in titles.items():
        fig, axes = plt.subplots(2, 2, figsize=(9.1, 4.5), constrained_layout=True)
        fig.suptitle(title, fontsize=11, fontweight="bold", color="#19354A")
        rows = [r for r in data if r["workload"] == workload]
        series = sorted(set((r["structure"], r["variant"]) for r in rows))
        for ax, field in zip(axes.flat, ["time_ms", "steps", "moves", "comparisons"]):
            for structure, variant in series:
                selected = sorted((r for r in rows if (r["structure"], r["variant"]) == (structure, variant)),
                                  key=lambda r: int(r["n"]))
                label = structure + (f" / {variant}" if variant != "-" else "")
                ax.plot([int(r["n"]) for r in selected], [float(r[field]) for r in selected],
                        label=label, color=COLORS[structure], linewidth=1.65,
                        linestyle="--" if variant == "middle" else "-",
                        marker="s" if variant == "middle" else "o", markersize=3.5,
                        markerfacecolor="white" if structure == "MyLinkedList" else COLORS[structure])
            all_zero = all(float(r[field]) == 0 for r in rows)
            axes_style(ax, field, all_zero)
            if all_zero:
                ax.set_yscale("linear")
                ax.set_ylim(-.15, 1)
                ax.set_yticks([0, 1])
                ax.text(.5, .55, "All series = 0", transform=ax.transAxes, ha="center", color="#536575")
            if workload == "W2" and field == "comparisons":
                ax.text(.02, .92, "Curves coincide", transform=ax.transAxes, fontsize=7)
        fig.savefig(OUT / f"{workload.lower()}.png", dpi=220)
        plt.close(fig)

    rows = read("build_heap.csv")
    fig, axes = plt.subplots(1, 2, figsize=(9.1, 2.55), constrained_layout=True)
    fig.suptitle("Bonus B | Floyd construction vs repeated insertion", fontsize=11, fontweight="bold", color="#19354A")
    for ax, field in zip(axes, ["time_ms", "comparisons"]):
        for method in ["RepeatedInsert", "Floyd"]:
            for variant in ["random", "descending"]:
                selected = sorted((r for r in rows if r["structure"] == method and r["variant"] == variant),
                                  key=lambda r: int(r["n"]))
                ax.plot([int(r["n"]) for r in selected], [float(r[field]) for r in selected],
                        color=COLORS[method], linestyle="--" if variant == "descending" else "-",
                        marker="s" if variant == "descending" else "o", markersize=3,
                        label=f"{method} / {variant}")
        axes_style(ax, field)
    fig.savefig(OUT / "build_heap.png", dpi=220)
    plt.close(fig)
    print("Created five plots in", OUT)

if __name__ == "__main__":
    main()
