package edu.daa;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Random;

public final class Benchmark {
    static final int WARMUPS = 3;
    static final int REPETITIONS = 5;
    static final int GLOBAL_WARMUP_PASSES = 25;
    static final int[] SIZES = {100, 1_000, 10_000, 100_000};
    private static volatile long blackhole;

    record Sample(long nanos, long steps, long moves, long comparisons, long checksum) {}

    static final class Data {
        final int[] values;
        final int[] indices = new int[10_000];
        final int[] queries = new int[1_000];
        final int[] inserted = new int[1_000];
        Data(int n) {
            if (n <= 0) throw new IllegalArgumentException("n must be positive");
            values = new int[n];
            Random valuesRandom = new Random(42);
            for (int i = 0; i < n; i++) values[i] = valuesRandom.nextInt(Integer.MAX_VALUE);
            Random queryRandom = new Random(42);
            for (int i = 0; i < indices.length; i++) indices[i] = queryRandom.nextInt(n);
            for (int i = 0; i < 500; i++) {
                queries[i] = values[queryRandom.nextInt(n)];
                queries[i + 500] = -1 - queryRandom.nextInt(Integer.MAX_VALUE);
            }

            for (int i = queries.length - 1; i > 0; i--) {
                int j = queryRandom.nextInt(i + 1);
                int t = queries[i]; queries[i] = queries[j]; queries[j] = t;
            }
            for (int i = 0; i < inserted.length; i++) inserted[i] = queryRandom.nextInt();
        }
    }

    public static void main(String[] args) throws IOException {
        Path directory = Path.of(args.length == 0 ? "results" : args[0]);
        Files.createDirectories(directory);
        prewarm();
        try (PrintWriter csv = writer(directory.resolve("results.csv"));
             PrintWriter raw = writer(directory.resolve("raw_samples.csv"))) {
            csv.println("workload,variant,structure,n,time_ms,steps,moves,comparisons");
            raw.println("workload,variant,structure,n,run,time_ns,steps,moves,comparisons,checksum");
            for (int sizeIndex = 0; sizeIndex < SIZES.length; sizeIndex++) {
                int n = SIZES[sizeIndex]; Data data = new Data(n);
                for (int workload = 1; workload <= 3; workload++) {
                    int variants = workload == 3 ? 2 : 1;
                    for (int variant = 0; variant < variants; variant++) {
                        String label = workload == 3 ? (variant == 0 ? "head" : "middle") : "-";
                        for (int order = 0; order < 2; order++) {

                            boolean linked = ((order + sizeIndex) % 2) == 1;
                            measure(csv, raw, workload, label, linked, data);
                        }
                    }
                }
                measure(csv, raw, 4, "-", false, data);
            }
            csv.flush(); raw.flush();
            if (csv.checkError() || raw.checkError()) throw new IOException("Cannot write benchmark CSV");
        }
        runBuildComparison(directory);
        String metadata = "timestamp_utc=" + Instant.now() + "\n"
                + "java_version=" + System.getProperty("java.version") + "\n"
                + "java_vm=" + System.getProperty("java.vm.name") + "\n"
                + "os=" + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n"
                + "architecture=" + System.getProperty("os.arch") + "\n"
                + "available_processors=" + Runtime.getRuntime().availableProcessors() + "\n"
                + "max_heap_bytes=" + Runtime.getRuntime().maxMemory() + "\n"
                + "seed=42\nwarmup_runs=3\nmeasured_runs=5\nstatistic=median\n"
                + "global_warmup_passes=" + GLOBAL_WARMUP_PASSES + "\n"
                + "timing=System.nanoTime; preparation and validation excluded\n"
                + "W3=1000 insertions followed by 1000 removals; fixed original n/2\n"
                + "blackhole=" + blackhole + "\n";
        Files.writeString(directory.resolve("environment.txt"), metadata, StandardCharsets.UTF_8);
        System.out.println("Saved 36 workload rows and 16 buildHeap comparison rows to " + directory);
    }

    private static void prewarm() {
        System.out.println("Warming all code paths before collecting measurements...");
        Data data = new Data(1_000);
        int[] descending = new int[1_000];
        for (int i = 0; i < descending.length; i++) descending[i] = descending.length - i;
        for (int pass = 0; pass < GLOBAL_WARMUP_PASSES; pass++) {
            for (boolean linked : new boolean[]{false, true}) {
                runCase(1, "-", linked, data);
                runCase(2, "-", linked, data);
                runCase(3, "head", linked, data);
                runCase(3, "middle", linked, data);
            }
            runCase(4, "-", false, data);
            for (boolean floyd : new boolean[]{false, true}) {
                buildCase(data.values, floyd); buildCase(descending, floyd);
            }
        }
    }

    private static void measure(PrintWriter csv, PrintWriter raw, int workload,
                                String variant, boolean linked, Data data) {
        for (int i = 0; i < WARMUPS; i++) runCase(workload, variant, linked, data);
        Sample[] samples = new Sample[REPETITIONS];
        for (int i = 0; i < REPETITIONS; i++) samples[i] = runCase(workload, variant, linked, data);
        String structure = workload == 4 ? "MinHeap" : linked ? "MyLinkedList" : "DynamicArray";
        writeSamples(csv, raw, "W" + workload, variant, structure, data.values.length, samples);
    }

    static Sample runCase(int workload, String variant, boolean linked, Data data) {
        if (workload == 4) return priorityCase(data);
        IntSequence sequence = linked ? new MyLinkedList() : new DynamicArray();
        for (int value : data.values) sequence.add(value);
        sequence.metrics().reset();
        int index = "head".equals(variant) ? 0 : data.values.length / 2;
        int[] removed = workload == 3 ? new int[data.inserted.length] : null;
        long checksum = 0;
        long start = System.nanoTime();
        switch (workload) {
            case 1 -> {
                for (int i : data.indices) checksum += sequence.get(i);
            }
            case 2 -> {
                for (int value : data.queries) if (sequence.contains(value)) checksum++;
            }
            case 3 -> {
                for (int value : data.inserted) sequence.add(index, value);
                for (int i = 0; i < removed.length; i++) {
                    removed[i] = sequence.remove(index); checksum += removed[i];
                }
            }
            default -> throw new IllegalArgumentException("Unknown workload");
        }
        long elapsed = System.nanoTime() - start;
        Sample sample = sample(elapsed, sequence.metrics(), checksum);

        if (sequence.size() != data.values.length) throw new AssertionError("Sequence size changed");
        if (workload == 1) {
            long expected = 0;
            for (int i : data.indices) expected += data.values[i];
            if (checksum != expected) throw new AssertionError("Random access result");
        } else if (workload == 2 && checksum != 500) {
            throw new AssertionError("Search must find exactly 500 queries");
        } else if (workload == 3) {
            for (int i = 0; i < removed.length; i++) {
                if (removed[i] != data.inserted[removed.length - 1 - i]) {
                    throw new AssertionError("Incorrect removal order");
                }
            }
            for (int i : new int[]{0, data.values.length / 2, data.values.length - 1}) {
                if (sequence.get(i) != data.values[i]) throw new AssertionError("Original values changed");
            }
        }
        blackhole ^= checksum;
        return sample;
    }

    private static Sample priorityCase(Data data) {
        MinHeap heap = new MinHeap();
        int[] output = new int[data.values.length];
        long start = System.nanoTime();
        for (int value : data.values) heap.insert(value);
        for (int i = 0; i < output.length; i++) output[i] = heap.extractMin();
        long elapsed = System.nanoTime() - start;
        long checksum = validateSorted(output, data.values);
        if (heap.size() != 0) throw new AssertionError("Heap must be empty");
        blackhole ^= checksum;
        return sample(elapsed, heap.metrics(), checksum);
    }

    static long validateSorted(int[] output, int[] original) {
        long sum = 0, expected = 0;
        for (int i = 0; i < output.length; i++) {
            if (i > 0 && output[i - 1] > output[i]) throw new AssertionError("Unsorted output");
            sum += output[i]; expected += original[i];
        }
        if (sum != expected) throw new AssertionError("Value checksum changed");
        return sum;
    }

    private static void runBuildComparison(Path directory) throws IOException {
        try (PrintWriter csv = writer(directory.resolve("build_heap.csv"));
             PrintWriter raw = writer(directory.resolve("build_heap_raw.csv"))) {
            csv.println("workload,variant,structure,n,time_ms,steps,moves,comparisons");
            raw.println("workload,variant,structure,n,run,time_ns,steps,moves,comparisons,checksum");
            for (int n : SIZES) {
                for (String order : new String[]{"random", "descending"}) {
                    int[] values = new Data(n).values;
                    if (order.equals("descending")) for (int i = 0; i < n; i++) values[i] = n - i;
                    for (boolean floyd : new boolean[]{false, true}) {
                        for (int i = 0; i < WARMUPS; i++) buildCase(values, floyd);
                        Sample[] samples = new Sample[REPETITIONS];
                        for (int i = 0; i < REPETITIONS; i++) samples[i] = buildCase(values, floyd);
                        writeSamples(csv, raw, "Build", order, floyd ? "Floyd" : "RepeatedInsert", n, samples);
                    }
                }
            }
            csv.flush(); raw.flush();
            if (csv.checkError() || raw.checkError()) throw new IOException("Cannot write build CSV");
        }
    }

    private static Sample buildCase(int[] values, boolean floyd) {
        MinHeap heap = new MinHeap();
        long start = System.nanoTime();
        if (floyd) heap.buildHeap(values);
        else for (int value : values) heap.insert(value);
        long elapsed = System.nanoTime() - start;
        Sample counts = sample(elapsed, heap.metrics(), 0);
        if (!heap.isValidHeap()) throw new AssertionError("Invalid built heap");
        int[] output = new int[values.length];
        for (int i = 0; i < output.length; i++) output[i] = heap.extractMin();
        long checksum = validateSorted(output, values); blackhole ^= checksum;
        return new Sample(counts.nanos, counts.steps, counts.moves, counts.comparisons, checksum);
    }

    private static Sample sample(long nanos, Metrics m, long checksum) {
        return new Sample(nanos, m.steps(), m.moves(), m.comparisons(), checksum);
    }

    private static void writeSamples(PrintWriter csv, PrintWriter raw, String workload,
                                     String variant, String structure, int n, Sample[] samples) {
        long[] times = new long[samples.length]; Sample first = samples[0];
        for (int i = 0; i < samples.length; i++) {
            Sample s = samples[i]; times[i] = s.nanos;
            if (s.steps != first.steps || s.moves != first.moves || s.comparisons != first.comparisons
                    || s.checksum != first.checksum) throw new AssertionError("Non-reproducible counters");
            raw.printf(Locale.ROOT, "%s,%s,%s,%d,%d,%d,%d,%d,%d,%d%n",
                    workload, variant, structure, n, i + 1, s.nanos, s.steps, s.moves, s.comparisons, s.checksum);
        }
        long median = median(times);
        csv.printf(Locale.ROOT, "%s,%s,%s,%d,%.6f,%d,%d,%d%n", workload, variant, structure,
                n, median / 1_000_000.0, first.steps, first.moves, first.comparisons);
        System.out.printf(Locale.ROOT, "%s %-6s %-14s n=%6d: %.4f ms%n",
                workload, variant, structure, n, median / 1_000_000.0);
    }

    static long median(long[] values) {

        for (int i = 1; i < values.length; i++) {
            long value = values[i]; int j = i - 1;
            while (j >= 0 && values[j] > value) { values[j + 1] = values[j]; j--; }
            values[j + 1] = value;
        }
        return values[values.length / 2];
    }

    private static PrintWriter writer(Path path) throws IOException {
        return new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8));
    }
}
