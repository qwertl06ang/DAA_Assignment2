package edu.daa;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DynamicArrayTest extends SequenceContractTest {
    @Override IntSequence create() { return new DynamicArray(); }

    @Test void capacityDoublesAndPreservesValues() {
        DynamicArray a = new DynamicArray(1);
        for (int i = 0; i < 1_024; i++) {
            int oldCapacity = a.capacity();
            boolean full = a.size() == oldCapacity;
            a.add(i);
            assertEquals(full ? 2 * oldCapacity : oldCapacity, a.capacity());
        }
        for (int i = 0; i < a.size(); i++) assertEquals(i, a.get(i));
    }

    @Test void zeroCapacityWorksAndNegativeCapacityFails() {
        DynamicArray a = new DynamicArray(0); a.add(6);
        assertEquals(6, a.get(0));
        assertThrows(IllegalArgumentException.class, () -> new DynamicArray(-1));
    }

    @Test void growthAndShiftCountersAreExact() {
        DynamicArray a = new DynamicArray(2); a.add(10); a.add(20);
        a.metrics().reset(); a.add(1, 30);
        assertCounts(a.metrics(), 3, 3, 0); // Two copies and one suffix shift.
        a.metrics().reset(); assertEquals(10, a.remove(0));
        assertCounts(a.metrics(), 3, 2, 0); // Returned cell and two shifts.
    }

    @Test void readsAndSearchCountersAreExact() {
        DynamicArray a = new DynamicArray(); a.add(10); a.add(20); a.add(30);
        a.metrics().reset(); assertEquals(30, a.get(2));
        assertCounts(a.metrics(), 1, 0, 0);
        a.metrics().reset(); assertTrue(a.contains(20));
        assertCounts(a.metrics(), 2, 0, 2);
        a.metrics().reset(); assertFalse(a.contains(-1));
        assertCounts(a.metrics(), 3, 0, 3);
        a.metrics().reset(); assertCounts(a.metrics(), 0, 0, 0);
    }

    static void assertCounts(Metrics m, long steps, long moves, long comparisons) {
        assertAll(() -> assertEquals(steps, m.steps()),
                () -> assertEquals(moves, m.moves()),
                () -> assertEquals(comparisons, m.comparisons()));
    }
}
