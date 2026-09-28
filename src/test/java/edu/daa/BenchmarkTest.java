package edu.daa;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

final class BenchmarkTest {
    @Test void dataHasExactHitMissBalanceAndValidIndices() {
        for (int n : Benchmark.SIZES) {
            Benchmark.Data data = new Benchmark.Data(n);
            HashSet<Integer> present = new HashSet<>();
            for (int value : data.values) { assertTrue(value >= 0); present.add(value); }
            int hits = 0;
            for (int value : data.queries) if (present.contains(value)) hits++;
            assertEquals(500, hits);
            for (int index : data.indices) assertTrue(index >= 0 && index < n);
            Benchmark.Data again = new Benchmark.Data(n);
            assertArrayEquals(data.values, again.values);
            assertArrayEquals(data.queries, again.queries);
            assertArrayEquals(data.indices, again.indices);
            assertArrayEquals(data.inserted, again.inserted);
        }
    }

    @Test void medianSelectsThirdOfFive() {
        assertEquals(7, Benchmark.median(new long[]{20, 1, 9, 7, 3}));
    }

    @Test void pairedWorkloadsHaveEqualChecksumsAndExpectedCounters() {
        Benchmark.Data data = new Benchmark.Data(100);
        for (int workload = 1; workload <= 3; workload++) {
            for (String variant : workload == 3 ? new String[]{"head", "middle"} : new String[]{"-"}) {
                Benchmark.Sample a = Benchmark.runCase(workload, variant, false, data);
                Benchmark.Sample l = Benchmark.runCase(workload, variant, true, data);
                assertEquals(a.checksum(), l.checksum());
                if (workload == 1) {
                    assertEquals(10_000, a.steps());
                    long sum = 0; for (int index : data.indices) sum += index;
                    assertEquals(sum, l.steps());
                } else if (workload == 2) {
                    assertEquals(a.comparisons(), l.comparisons());
                    assertEquals(500, a.checksum());
                } else {
                    assertEquals(3_000, l.moves());
                    assertEquals(variant.equals("head") ? 1_000 : 99_000, l.steps());
                }
            }
        }
    }

    @Test void priorityWorkloadRunsAndValidates() {
        assertTrue(Benchmark.runCase(4, "-", false, new Benchmark.Data(1_000)).comparisons() > 0);
        assertThrows(AssertionError.class, () -> Benchmark.validateSorted(new int[]{2, 1}, new int[]{1, 2}));
        assertThrows(AssertionError.class, () -> Benchmark.validateSorted(new int[]{1, 2}, new int[]{1, 3}));
    }
}
