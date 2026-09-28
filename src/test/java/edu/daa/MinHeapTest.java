package edu.daa;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import static edu.daa.DynamicArrayTest.assertCounts;

final class MinHeapTest {
    @Test void emptyOperationsThrow() {
        MinHeap h = new MinHeap();
        assertThrows(IllegalStateException.class, h::peekMin);
        assertThrows(IllegalStateException.class, h::extractMin);
        assertTrue(h.isValidHeap());
    }

    @Test void singletonAndReuse() {
        MinHeap h = new MinHeap(0); h.insert(7);
        assertEquals(7, h.peekMin()); assertEquals(1, h.size());
        assertEquals(7, h.extractMin()); assertEquals(0, h.size());
        h.insert(-8); assertEquals(-8, h.extractMin());
        assertTrue(h.isValidHeap());
    }

    @Test void duplicatesAndIntegerExtremes() {
        checkSorted(new int[]{Integer.MAX_VALUE, -1, 0, Integer.MIN_VALUE, -1, 0});
        checkSorted(new int[]{5, 5, 5, 5, 5});
    }

    @Test void ascendingDescendingAndSingleChild() {
        int[] ascending = new int[301];
        int[] descending = new int[301];
        for (int i = 0; i < 301; i++) { ascending[i] = i; descending[i] = 301 - i; }
        checkSorted(ascending); checkSorted(descending); checkSorted(new int[]{2, 1});
    }

    @Test void randomSortedOutputAndPropertyAfterEveryOperation() {
        int[] values = new int[2_000]; Random random = new Random(42);
        for (int i = 0; i < values.length; i++) values[i] = random.nextInt();
        checkSorted(values);
    }

    @Test void randomMixedOperationsMatchPriorityQueue() {
        MinHeap h = new MinHeap(1); PriorityQueue<Integer> expected = new PriorityQueue<>();
        Random random = new Random(42);
        for (int i = 0; i < 5_000; i++) {
            if (expected.isEmpty() || random.nextBoolean()) {
                int value = random.nextInt(101) - 50; h.insert(value); expected.add(value);
            } else {
                assertEquals(expected.remove().intValue(), h.extractMin());
            }
            assertEquals(expected.size(), h.size());
            assertTrue(h.isValidHeap(), "Heap property at operation " + i);
            if (!expected.isEmpty()) assertEquals(expected.peek().intValue(), h.peekMin());
        }
        while (!expected.isEmpty()) {
            assertEquals(expected.remove().intValue(), h.extractMin());
            assertTrue(h.isValidHeap());
        }
    }

    @Test void smallHeapCountersAreExact() {
        MinHeap h = new MinHeap(2); h.insert(5);
        assertCounts(h.metrics(), 0, 0, 0);
        h.metrics().reset(); h.insert(3);
        assertCounts(h.metrics(), 2, 2, 1);
        h.metrics().reset(); assertEquals(3, h.peekMin());
        assertCounts(h.metrics(), 1, 0, 0);
        h.metrics().reset(); assertEquals(3, h.extractMin());
        assertCounts(h.metrics(), 2, 1, 0);
        h.metrics().reset(); assertEquals(5, h.extractMin());
        assertCounts(h.metrics(), 1, 0, 0);
    }

    @Test void resizingAndBubbleDownCountersAreExact() {
        MinHeap h = new MinHeap(2); h.insert(1); h.insert(2);
        h.metrics().reset(); h.insert(3);
        assertCounts(h.metrics(), 4, 2, 1);
        h.metrics().reset(); assertEquals(1, h.extractMin());
        assertCounts(h.metrics(), 4, 3, 1);
        assertTrue(h.isValidHeap());
        assertCounts(h.metrics(), 4, 3, 1);
    }

    @Test void buildHeapCopiesInputAndReplacesOldContents() {
        MinHeap h = new MinHeap(); h.insert(-99);
        int[] values = {8, 3, 5, 3, -2}; int[] saved = values.clone();
        h.buildHeap(values); assertArrayEquals(saved, values); assertTrue(h.isValidHeap());
        values[0] = Integer.MIN_VALUE;
        Arrays.sort(saved);
        for (int value : saved) {
            assertEquals(value, h.extractMin()); assertTrue(h.isValidHeap());
        }
        assertEquals(0, h.size());
    }

    @Test void buildHeapEmptySingletonAndRandomInputs() {
        MinHeap h = new MinHeap(); h.insert(9); h.buildHeap(new int[0]);
        assertEquals(0, h.size()); assertTrue(h.isValidHeap());
        Random r = new Random(42);
        for (int n : new int[]{1, 2, 3, 100, 1_000}) {
            int[] values = r.ints(n).toArray(); h.buildHeap(values);
            assertTrue(h.isValidHeap()); Arrays.sort(values);
            for (int value : values) {
                assertEquals(value, h.extractMin()); assertTrue(h.isValidHeap());
            }
        }
    }

    @Test void invalidConstructorAndNullBuildInput() {
        assertThrows(IllegalArgumentException.class, () -> new MinHeap(-1));
        MinHeap h = new MinHeap(); h.insert(8);
        assertThrows(NullPointerException.class, () -> h.buildHeap(null));
        assertEquals(8, h.peekMin());
    }

    private static void checkSorted(int[] values) {
        MinHeap h = new MinHeap(1);
        for (int value : values) { h.insert(value); assertTrue(h.isValidHeap()); }
        int[] expected = values.clone(); Arrays.sort(expected);
        for (int value : expected) {
            assertEquals(value, h.peekMin()); assertEquals(value, h.extractMin());
            assertTrue(h.isValidHeap());
        }
        assertEquals(0, h.size());
    }
}
