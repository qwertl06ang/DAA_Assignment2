package edu.daa;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

abstract class SequenceContractTest {
    abstract IntSequence create();

    @Test void emptySequence() {
        IntSequence s = create();
        assertEquals(0, s.size());
        assertFalse(s.contains(10));
        assertThrows(IndexOutOfBoundsException.class, () -> s.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> s.remove(0));
    }

    @Test void singletonCanBeRemovedAndReused() {
        IntSequence s = create();
        s.add(0, 7);
        assertEquals(7, s.get(0));
        assertTrue(s.contains(7));
        assertEquals(7, s.remove(0));
        assertEquals(0, s.size());
        s.add(9);
        assertEquals(9, s.get(0));
    }

    @Test void duplicateValuesRemainAfterOneRemoval() {
        IntSequence s = create();
        s.add(4); s.add(4); s.add(4);
        assertEquals(4, s.remove(1));
        assertEquals(2, s.size());
        assertTrue(s.contains(4));
        assertEquals(4, s.get(1));
    }

    @Test void firstMiddleAndLastPositions() {
        IntSequence s = create();
        s.add(10); s.add(30); s.add(1, 20); s.add(0, 5); s.add(s.size(), 40);
        int[] expected = {5, 10, 20, 30, 40};
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], s.get(i));
        assertEquals(40, s.remove(4));
        assertEquals(5, s.remove(0));
        assertEquals(20, s.remove(1));
        s.add(50);
        assertEquals(50, s.get(2));
    }

    @Test void invalidIndicesDoNotChangeContents() {
        IntSequence s = create(); s.add(8);
        for (int index : new int[]{-1, 1, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> s.get(index));
            assertThrows(IndexOutOfBoundsException.class, () -> s.remove(index));
        }
        for (int index : new int[]{-1, 2, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> s.add(index, 3));
        }
        assertEquals(1, s.size()); assertEquals(8, s.get(0));
    }

    @Test void intExtremes() {
        IntSequence s = create();
        s.add(Integer.MIN_VALUE); s.add(0); s.add(Integer.MAX_VALUE);
        assertTrue(s.contains(Integer.MIN_VALUE));
        assertTrue(s.contains(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, s.remove(0));
        assertEquals(Integer.MAX_VALUE, s.remove(1));
    }

    @Test void randomOperationsMatchArrayList() {
        IntSequence actual = create();
        ArrayList<Integer> expected = new ArrayList<>();
        Random random = new Random(42);
        for (int operation = 0; operation < 5_000; operation++) {
            int value = random.nextInt(51) - 25;
            switch (random.nextInt(5)) {
                case 0 -> { actual.add(value); expected.add(value); }
                case 1 -> {
                    int i = random.nextInt(expected.size() + 1);
                    actual.add(i, value); expected.add(i, value);
                }
                case 2 -> {
                    if (!expected.isEmpty()) {
                        int i = random.nextInt(expected.size());
                        assertEquals(expected.remove(i).intValue(), actual.remove(i));
                    }
                }
                case 3 -> {
                    if (!expected.isEmpty()) {
                        int i = random.nextInt(expected.size());
                        assertEquals(expected.get(i).intValue(), actual.get(i));
                    }
                }
                case 4 -> assertEquals(expected.contains(value), actual.contains(value));
                default -> throw new AssertionError();
            }
            assertEquals(expected.size(), actual.size());
            if (operation % 127 == 0) {
                for (int i = 0; i < expected.size(); i++) {
                    assertEquals(expected.get(i).intValue(), actual.get(i));
                }
            }
        }
        for (int i = 0; i < expected.size(); i++) assertEquals(expected.get(i).intValue(), actual.get(i));
    }

    @Test void repeatedHeadAndMiddleEditsRestoreSequence() {
        for (int index : new int[]{0, 50}) {
            IntSequence s = create();
            for (int i = 0; i < 100; i++) s.add(i);
            for (int i = 0; i < 1_000; i++) s.add(index, -i - 1);
            for (int i = 999; i >= 0; i--) assertEquals(-i - 1, s.remove(index));
            for (int i = 0; i < 100; i++) assertEquals(i, s.get(i));
        }
    }
}
