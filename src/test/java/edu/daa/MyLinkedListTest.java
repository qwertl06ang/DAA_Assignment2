package edu.daa;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static edu.daa.DynamicArrayTest.assertCounts;

final class MyLinkedListTest extends SequenceContractTest {
    @Override IntSequence create() { return new MyLinkedList(); }

    @Test void appendAndTraversalCountersAreExact() {
        MyLinkedList s = new MyLinkedList(); s.add(1);
        assertCounts(s.metrics(), 0, 2, 0);
        s.metrics().reset(); s.add(2);
        assertCounts(s.metrics(), 0, 2, 0);
        s.metrics().reset(); assertEquals(2, s.get(1));
        assertCounts(s.metrics(), 1, 0, 0);
        s.metrics().reset(); assertEquals(1, s.get(0));
        assertCounts(s.metrics(), 0, 0, 0);
    }

    @Test void headEditsCountPointerUpdates() {
        MyLinkedList s = new MyLinkedList(); s.add(1); s.add(2);
        s.metrics().reset(); s.add(0, 3);
        assertCounts(s.metrics(), 0, 2, 0);
        s.metrics().reset(); assertEquals(3, s.remove(0));
        assertCounts(s.metrics(), 1, 1, 0);
    }

    @Test void middleAndTailEditsCountTraversalAndRelinking() {
        MyLinkedList s = new MyLinkedList();
        for (int i = 0; i < 4; i++) s.add(i);
        s.metrics().reset(); s.add(2, 9);
        assertCounts(s.metrics(), 1, 2, 0);
        s.metrics().reset(); assertEquals(9, s.remove(2));
        assertCounts(s.metrics(), 2, 1, 0);
        s.metrics().reset(); assertEquals(3, s.remove(3));
        assertCounts(s.metrics(), 3, 2, 0);
        s.add(4); assertEquals(4, s.get(3));
    }

    @Test void containsCountsTerminalNullTraversal() {
        MyLinkedList s = new MyLinkedList(); s.add(1); s.add(2); s.add(3);
        s.metrics().reset(); assertTrue(s.contains(2));
        assertCounts(s.metrics(), 1, 0, 2);
        s.metrics().reset(); assertFalse(s.contains(99));
        assertCounts(s.metrics(), 3, 0, 3);
    }
}
