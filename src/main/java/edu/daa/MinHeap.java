package edu.daa;

/** Array-based binary min-heap; values are primitive ints. */
public final class MinHeap {
    private int[] data;
    private int size;
    private final Metrics metrics = new Metrics();

    public MinHeap() { this(16); }
    public MinHeap(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("Negative capacity");
        data = new int[Math.max(1, initialCapacity)];
    }

    public int size() { return size; }
    public Metrics metrics() { return metrics; }

    public void insert(int value) {
        ensureCapacity();
        int index = size++;
        data[index] = value;
        while (index > 0) {
            int parent = (index - 1) / 2;
            int childValue = read(index);
            int parentValue = read(parent);
            metrics.compare();
            if (parentValue <= childValue) break;
            data[parent] = childValue; metrics.move();
            data[index] = parentValue; metrics.move();
            index = parent;
        }
    }

    public int peekMin() {
        checkNotEmpty();
        return read(0);
    }

    public int extractMin() {
        checkNotEmpty();
        int result = read(0);
        size--;
        if (size > 0) {
            data[0] = read(size); metrics.move();
            bubbleDown(0);
        }
        return result;
    }

    /** Replaces this heap with an independent copy, then applies Floyd's algorithm. */
    public void buildHeap(int[] values) {
        if (values == null) throw new NullPointerException("values");
        data = new int[Math.max(1, values.length)];
        size = values.length;
        for (int i = 0; i < size; i++) {
            metrics.step(); // A read from the supplied primitive array.
            data[i] = values[i]; metrics.move();
        }
        for (int i = size / 2 - 1; i >= 0; i--) bubbleDown(i);
    }

    private void bubbleDown(int index) {
        // Both child subtrees are heaps; only this node may violate their roots.
        while (index < size / 2) {
            int child = index * 2 + 1;
            int childValue = read(child);
            int right = child + 1;
            if (right < size) {
                int rightValue = read(right);
                metrics.compare();
                if (rightValue < childValue) {
                    child = right;
                    childValue = rightValue;
                }
            }
            int parentValue = read(index);
            metrics.compare();
            if (parentValue <= childValue) break;
            data[index] = childValue; metrics.move();
            data[child] = parentValue; metrics.move();
            index = child;
        }
    }

    private int read(int index) {
        metrics.step();
        return data[index];
    }

    private void ensureCapacity() {
        if (size < data.length) return;
        if (data.length > (Integer.MAX_VALUE - 8) / 2) {
            throw new OutOfMemoryError("Cannot double heap capacity");
        }
        int[] larger = new int[data.length * 2];
        for (int i = 0; i < size; i++) {
            larger[i] = read(i); metrics.move();
        }
        data = larger;
    }

    private void checkNotEmpty() {
        if (size == 0) throw new IllegalStateException("Heap is empty");
    }

    /** Test diagnostic: deliberately outside the measured operation counters. */
    boolean isValidHeap() {
        for (int child = 1; child < size; child++) {
            if (data[(child - 1) / 2] > data[child]) return false;
        }
        return true;
    }
}
