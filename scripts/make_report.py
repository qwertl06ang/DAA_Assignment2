"""Generate the five-page report and its Markdown companion from measured CSVs."""
from pathlib import Path
import csv
import html
import re
import matplotlib
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak

ROOT = Path(__file__).resolve().parents[1]
FONTS = Path(matplotlib.get_data_path()) / "fonts" / "ttf"
pdfmetrics.registerFont(TTFont("Report", str(FONTS / "DejaVuSans.ttf")))
pdfmetrics.registerFont(TTFont("ReportBold", str(FONTS / "DejaVuSans-Bold.ttf")))
pdfmetrics.registerFontFamily("Report", normal="Report", bold="ReportBold")
NAVY = colors.HexColor("#19354A")
BLUE = colors.HexColor("#166B9A")
GRAY = colors.HexColor("#566777")
WIDTH = A4[0] - 84
STYLES = {
    "title": ParagraphStyle("title", fontName="ReportBold", fontSize=22, leading=27,
                            textColor=NAVY, spaceAfter=5),
    "h1": ParagraphStyle("h1", fontName="ReportBold", fontSize=14, leading=18,
                         textColor=NAVY, spaceBefore=3, spaceAfter=8),
    "h2": ParagraphStyle("h2", fontName="ReportBold", fontSize=10.5, leading=14,
                         textColor=BLUE, spaceBefore=8, spaceAfter=4),
    "body": ParagraphStyle("body", fontName="Report", fontSize=8.9, leading=12.5,
                           textColor=NAVY, spaceAfter=6),
    "small": ParagraphStyle("small", fontName="Report", fontSize=7.7, leading=10.3,
                            textColor=GRAY, spaceAfter=5),
    "table": ParagraphStyle("table", fontName="Report", fontSize=7.1, leading=9.4,
                            textColor=NAVY),
    "tablehead": ParagraphStyle("tablehead", fontName="ReportBold", fontSize=7.2, leading=9.5,
                                textColor=colors.white),
}


def load(name):
    with (ROOT / "results" / name).open(encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


DATA = load("results.csv")
BUILD = load("build_heap.csv")
ENV = dict(line.split("=", 1) for line in (ROOT / "results" / "environment.txt").read_text().splitlines() if "=" in line)
story, md = [], []


def row(w, s, variant="-", source=DATA, n=100000):
    return next(r for r in source if r["workload"] == w and r["structure"] == s
                and r["variant"] == variant and int(r["n"]) == n)


def number(r, field="time_ms", digits=4):
    return f"{float(r[field]):,.{digits}f}" if field == "time_ms" else f"{int(r[field]):,}"


def plain(text):
    return html.unescape(re.sub("<[^>]+>", "", text))


def p(text, small=False):
    story.append(Paragraph(text, STYLES["small" if small else "body"]))
    md.append(plain(text) + "\n")


def heading(text, level=1):
    story.append(Paragraph(text, STYLES["h1" if level == 1 else "h2"]))
    md.append("#" * (level + 1) + " " + text + "\n")


def table(headers, rows, widths):
    cells = [[Paragraph(html.escape(str(x)), STYLES["tablehead"]) for x in headers]]
    cells += [[Paragraph(html.escape(str(x)), STYLES["table"]) for x in r] for r in rows]
    t = Table(cells, colWidths=widths, repeatRows=1, hAlign="LEFT")
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.HexColor("#F0F5F8"), colors.white]),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LINEBELOW", (0, 0), (-1, 0), .6, BLUE),
    ]))
    story.extend([t, Spacer(1, 7)])
    md.append("| " + " | ".join(headers) + " |")
    md.append("| " + " | ".join(["---"] * len(headers)) + " |")
    md.extend("| " + " | ".join(map(str, r)) + " |" for r in rows)
    md.append("")


def figure(filename, caption, ratio):
    story.append(Image(str(ROOT / "results" / "plots" / filename), width=WIDTH, height=WIDTH / ratio))
    story.append(Spacer(1, 3))
    md.append(f"![{plain(caption)}](results/plots/{filename})\n")
    p(caption, small=True)


def new_page():
    story.append(PageBreak())
    md.append("\n---\n")


def footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(colors.HexColor("#D5E1E8"))
    canvas.line(42, 32, A4[0] - 42, 32)
    canvas.setFont("Report", 7.3)
    canvas.setFillColor(GRAY)
    canvas.drawString(42, 20, "DAA Assignment 2 | Adilzhan Aliakbar | SE-2521")
    canvas.drawRightString(A4[0] - 42, 20, f"{doc.page} / 5")
    canvas.restoreState()


# PAGE 1
story.append(Paragraph("In-Memory Workload Engine", STYLES["title"]))
md.append("# In-Memory Workload Engine\n")
p("<b>Assignment 2 - Data Structures</b><br/>Adilzhan Aliakbar | SE-2521 | 28 September 2026")
p("Three implementations store primitive ints: a doubling dynamic array, a singly linked list with head and tail, "
  "and an array-based binary min-heap. Both sequences implement IntSequence. Production code uses no Java collection classes.")
heading("1. Complexity and storage")
complexity = [
    ["Array: add(x)", "Θ(1)", "Θ(1)*", "Θ(n)", "1 / n", "Append; full capacity triggers doubling."],
    ["Array: add(i,x)", "Θ(1)", "Θ(n)", "Θ(n)", "1 / n", "Shift n-i values; may also grow."],
    ["Array: remove(i)", "Θ(1)", "Θ(n)", "Θ(n)", "1", "Shift n-i-1 values; no shrinking."],
    ["Array: get(i)", "Θ(1)", "Θ(1)", "Θ(1)", "1", "One direct cell read."],
    ["Array: contains(x)", "Θ(1)", "Θ(n)", "Θ(n)", "1", "Scan to first match or end."],
    ["List: add(x)", "Θ(1)", "Θ(1)", "Θ(1)", "1", "Tail pointer avoids traversal."],
    ["List: add(i,x)", "Θ(1)", "Θ(n)", "Θ(n)", "1", "Endpoints fast; otherwise find i-1."],
    ["List: remove(i)", "Θ(1)", "Θ(n)", "Θ(n)", "1", "Head fast; otherwise find predecessor."],
    ["List: get(i)", "Θ(1)", "Θ(n)", "Θ(n)", "1", "Follow i next references from head."],
    ["List: contains(x)", "Θ(1)", "Θ(n)", "Θ(n)", "1", "Inspect nodes until match or null."],
    ["Heap: insert(x)", "Θ(1)", "O(log n)*", "Θ(n)", "1 / n", "At most log n swaps; growth copies n."],
    ["Heap: peekMin()", "Θ(1)", "Θ(1)", "Θ(1)", "1", "Read root at index zero."],
    ["Heap: extractMin()", "Θ(1)", "Θ(log n)", "Θ(log n)", "1", "Replace root, descend; equal keys may stop early."],
    ["Heap: buildHeap(a)", "Θ(n)", "Θ(n)", "Θ(n)", "n", "Copy input, then sift internal nodes bottom-up."],
]
table(["Operation", "Best", "Average", "Worst", "Aux. Θ", "Justification"], complexity,
      [108, 41, 59, 48, 40, WIDTH - 296])
p("<b>Models:</b> n ≥ 2; indexed operations average over uniform valid indices. Search averages use a constant miss "
  "fraction (50% in W2); heap extraction assumes random distinct priorities. The best heap extraction can be constant "
  "with equal keys. Invalid-input rejection is Θ(1).", small=True)
p("<b>* Growth and averages:</b> for append/insert, the average is over calls in a growing sequence, including allocation "
  "costs (amortized), not the expected cost at a known full capacity. Array append has tight amortized Θ(1). "
  "Heap insert has amortized Ω(1) and O(log n), depending on key order; no tight statistical average is claimed. "
  "At a full capacity either insertion costs Θ(n).", small=True)
p("<b>Auxiliary space:</b> 1 / n means Θ(1) without growth, Θ(n) during copying. The list retains Θ(n) nodes. "
  "The arrays retain Θ(capacity) cells; deletions do not shrink capacity, so storage need not be Θ(current n). "
  "Accessors and counter reset use Θ(1) time and space.", small=True)

# PAGE 2
new_page()
heading("2. Loop-invariant proofs")
heading("A. DynamicArray.contains(value)", 2)
p("<b>Invariant.</b> Before iteration i, 0 ≤ i ≤ size and no entry in data[0..i) equals value; the array is unchanged. "
  "<b>Initialization.</b> At i = 0 the inspected prefix is empty, so the statement is true.")
p("<b>Maintenance.</b> The loop reads data[i]. If it equals value, returning true supplies an actual matching element. "
  "Otherwise data[i] is also different, so incrementing i preserves the invariant for the larger prefix. "
  "<b>Termination.</b> size-i decreases on every continuing iteration. Normal termination has i = size, so every "
  "stored element differs and false is correct. <b>Conclusion.</b> Both return paths satisfy the specification, "
  "including an empty array; counters do not modify the stored values.")
heading("B. DynamicArray.remove(index)", 2)
p("Let k be the checked index, m the original size, and A the original logical array. The method saves A[k] before shifting. "
  "<b>Invariant.</b> Before iteration i: k ≤ i ≤ m-1; data[j] = A[j] for j &lt; k; data[j] = A[j+1] "
  "for k ≤ j &lt; i; and data[j] = A[j] for i ≤ j &lt; m. The size remains m during the loop.")
p("<b>Initialization.</b> i = k, so the shifted interval is empty and every cell still holds its original value. "
  "<b>Maintenance.</b> If i &lt; m-1, data[i+1] is still A[i+1]. Assigning it to data[i] extends the shifted "
  "interval by one; incrementing i preserves the untouched suffix. <b>Termination.</b> m-1-i decreases to zero. "
  "At i = m-1, positions k through m-2 contain A[k+1] through A[m-1]. Decrementing size excludes the stale last cell. "
  "<b>Conclusion.</b> The method returns A[k], preserves order, and leaves exactly A without its k-th element; "
  "removing the last or only element correctly executes zero shift iterations.")
heading("3. Measurement method")
p("Sizes are 100, 1,000, 10,000 and 100,000. Each size uses new Random(42); both sequences share the same values, "
  "indices and queries. W1 performs 10,000 gets. W2 shuffles 500 present values with 500 negative, guaranteed absent "
  "values. W3 performs all 1,000 insertions before 1,000 removals at 0 or the fixed original n/2. "
  "W4 inserts n values, then extracts n minima and validates non-decreasing output.")
p("All paths receive 25 global warm-up passes at n = 1,000; each case then discards three additional runs and measures "
  "five fresh runs. The CSV reports their median batch time from System.nanoTime. Initial fill for W1-W3, data creation, "
  "validation and CSV output are excluded. W4 includes insertion and extraction; its output buffer is allocated beforehand. "
  "Checksums feed a volatile sink. Counters must match across all five samples; 180 raw workload samples preserve the evidence.")
p("<b>Counter model.</b> A step is an array-cell read or traversal of one next link, including a terminal null. "
  "A move is an existing element relocation or an explicit structural head/tail/next assignment; a heap swap is two moves. "
  "New array-value placement and implicit initialization are excluded. Comparisons count stored int values only. "
  "Tests verify these conventions; counters are algorithm-level events, not CPU instructions or cache misses.")
p(f"<b>Measured environment:</b> Java {ENV['java_version']}, {ENV['java_vm']}; {ENV['os']}, {ENV['architecture']}; "
  f"{ENV['available_processors']} visible logical processors; -Xms256m -Xmx1g. Run recorded at {ENV['timestamp_utc']}. "
  "This is one JVM process on a shared workstation, not an isolated JMH study; short timings remain sensitive to JIT and scheduling.", small=True)

# PAGE 3
new_page()
heading("4. Read and search workloads")
p("Each figure shows median time and all three counters. The x-axis is logarithmic; time and positive counts use "
  "logarithmic y-axes. All-zero counter panels use a linear y-axis so zero operations remain visible.", small=True)
a, l = row("W1", "DynamicArray"), row("W1", "MyLinkedList")
figure("w1.png", f"Figure 1. W1 at n = 100,000: array {number(a)} ms and {number(a, 'steps')} reads; "
       f"list {number(l)} ms and {number(l, 'steps')} link traversals. The fixed query count keeps array steps constant.", 9.1 / 4.5)
a, l = row("W2", "DynamicArray"), row("W2", "MyLinkedList")
figure("w2.png", f"Figure 2. W2 at n = 100,000: array {number(a)} ms, list {number(l)} ms; both perform "
       f"{number(a, 'comparisons')} comparisons. The list has exactly 500 fewer steps because each successful query returns "
       "without traversing past its matching node. Zero moves and coincident comparison curves are intentional.", 9.1 / 4.5)

# PAGE 4
new_page()
heading("5. Mutation and priority workloads")
a, l = row("W3", "DynamicArray", "head"), row("W3", "MyLinkedList", "head")
figure("w3.png", f"Figure 3. At n = 100,000, head edits take {number(a)} ms in the array versus {number(l)} ms in the list. "
       "List head edits require 1,000 traversals and 3,000 link updates, independent of initial n. "
       "For k = 1,000 and fixed index i, array moves are 2k(n-i)+k(k-1), plus any growth copies; "
       "array steps add k reads of removed values. Middle-list traversal is 1,000(n-1) for these even sizes.", 9.1 / 4.5)
h = row("W4", "MinHeap")
figure("w4.png", f"Figure 4. At n = 100,000, priority processing takes {number(h)} ms with {number(h, 'steps')} steps, "
       f"{number(h, 'moves')} moves and {number(h, 'comparisons')} comparisons. Only MinHeap belongs to W4. "
       "Random-input batch work grows on the n log n scale; resizing adds a linear total number of copies.", 9.1 / 4.5)

# PAGE 5
new_page()
heading("6. Discussion")
a, l = row("W2", "DynamicArray"), row("W2", "MyLinkedList")
am, lm = row("W3", "DynamicArray", "middle"), row("W3", "MyLinkedList", "middle")
discussion = [
    "DynamicArray computes a cell address directly, so W1 uses one array read per get while the singly linked list follows roughly n/2 links for a uniform random index.",
    "Sequential scans also favor an int array because adjacent values share contiguous storage and cache lines.",
    "This spatial locality makes hardware prefetching more effective than dependent pointer chasing.",
    "A list node is a separate object containing a header, an int and a next reference, with possible alignment padding.",
    "Its next address is learned from the current node, limiting independent memory accesses even when asymptotic work is the same.",
    f"In W2 at n = 100,000 the comparison counts are identical, but the list takes {float(l['time_ms']) / float(a['time_ms']):.2f} times the array time.",
    "Per-node allocations can also increase memory management and garbage-collection pressure, although this experiment does not measure GC pauses or cache misses directly.",
    "The list is the better choice for repeated head insertions/removals, as its link work is independent of n.",
    "Its tail pointer supports constant-time append, but deleting the last node still requires locating its predecessor.",
    f"For middle edits at n = 100,000, the array takes {number(am)} ms versus the list's {number(lm)} ms despite both performing linear work per edit.",
    "MinHeap is preferable for repeated minimum-priority scheduling: peek is constant time and extraction is logarithmic in the worst case.",
    "For index-heavy or scan-heavy data, the array is preferable; measured constants should still be interpreted with the JVM warm-up, instrumentation and machine-load limitations in mind.",
]
p(" ".join(discussion))
heading("7. Bonus B: Floyd's bottom-up buildHeap")
fr, ir = row("Build", "Floyd", "random", BUILD), row("Build", "RepeatedInsert", "random", BUILD)
fd, it = row("Build", "Floyd", "descending", BUILD), row("Build", "RepeatedInsert", "descending", BUILD)
p("Leaves already satisfy the heap property; process internal nodes from n/2-1 down to zero. A node of height h "
  "costs O(h), and at most O(n/2^(h+1)) nodes have that height. Thus total sift work is "
  "O(n Σ h/2^h) = O(n); copying every input value gives Ω(n), hence Θ(n). "
  "Repeated insertion has Θ(n log n) worst-case work on descending input, but random insertion can show nearly linear comparison growth.")
figure("build_heap.png", f"Figure 5. At n = 100,000, random-input comparisons: Floyd {number(fr, 'comparisons')} versus inserts "
       f"{number(ir, 'comparisons')}; descending-input comparisons: {number(fd, 'comparisons')} versus {number(it, 'comparisons')}. "
       f"Descending times: {number(fd)} ms versus {number(it)} ms. Both methods include construction only; validation is excluded.", 9.1 / 2.55)
p("<b>Validation and reproducibility.</b> 39 JUnit 5 tests passed, with no failures, errors or skipped tests. "
  "CSV checks confirm every required case and exact medians from five samples. README documents execution and the local "
  "Git history with four feature branches, main and v1.0; GitHub publication is still pending. "
  "The optional JOL memory-footprint bonus was not attempted.", small=True)

doc = SimpleDocTemplate(str(ROOT / "REPORT.pdf"), pagesize=A4, leftMargin=42, rightMargin=42,
                        topMargin=36, bottomMargin=42, title="DAA Assignment 2 - Adilzhan Aliakbar",
                        author="Adilzhan Aliakbar", pageCompression=1)
doc.build(story, onFirstPage=footer, onLaterPages=footer)
(ROOT / "REPORT.md").write_text("\n".join(md), encoding="utf-8")
print("Created REPORT.pdf and REPORT.md")
