package edu.daa;

/** A contiguous int array that doubles its capacity and never shrinks. */
public final class DynamicArray implements IntSequence {
    private int[] data;
    private int size;
    private final Metrics metrics = new Metrics();

    public DynamicArray() { this(16); }

    public DynamicArray(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("Negative capacity");
        data = new int[Math.max(1, initialCapacity)];
    }

    @Override public int size() { return size; }
    @Override public Metrics metrics() { return metrics; }
    public int capacity() { return data.length; }

    @Override public void add(int value) {
        ensureCapacity();
        data[size++] = value; // A new value is not a relocation of an existing value.
    }

    @Override public void add(int index, int value) {
        checkPosition(index);
        ensureCapacity();
        for (int i = size; i > index; i--) {
            data[i] = read(i - 1);
            metrics.move();
        }
        data[index] = value;
        size++;
    }

    @Override public int remove(int index) {
        checkIndex(index);
        int removed = read(index);
        // Before iteration i, [index, i) already contains the shifted suffix.
        for (int i = index; i < size - 1; i++) {
            data[i] = read(i + 1);
            metrics.move();
        }
        size--;
        return removed;
    }

    @Override public int get(int index) {
        checkIndex(index);
        return read(index);
    }

    @Override public boolean contains(int value) {
        // Before iteration i, no element in [0, i) equals value.
        for (int i = 0; i < size; i++) {
            int candidate = read(i);
            metrics.compare();
            if (candidate == value) return true;
        }
        return false;
    }

    private int read(int index) {
        metrics.step();
        return data[index];
    }

    private void ensureCapacity() {
        if (size < data.length) return;
        if (data.length > (Integer.MAX_VALUE - 8) / 2) {
            throw new OutOfMemoryError("Cannot double array capacity");
        }
        int[] larger = new int[data.length * 2];
        for (int i = 0; i < size; i++) {
            larger[i] = read(i);
            metrics.move();
        }
        data = larger;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
    }

    private void checkPosition(int index) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException(index);
    }
}
