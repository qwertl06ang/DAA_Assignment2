package edu.daa;

/** The shared primitive-int API used by both sequence benchmarks. */
public interface IntSequence {
    int size();
    void add(int value);
    void add(int index, int value);
    int remove(int index);
    int get(int index);
    boolean contains(int value);
    Metrics metrics();
}
