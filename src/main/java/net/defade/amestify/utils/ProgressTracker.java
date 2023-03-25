package net.defade.amestify.utils;

public class ProgressTracker {
    private int total = 0;
    private int current = 0;

    public void reset(int total) {
        this.total = total;
        this.current = 0;
    }

    public void increment() {
        current++;
    }

    public void increment(int amount) {
        current += amount;
    }

    public float getProgress() {
        return (float) current / total;
    }

    public int getTotal() {
        return total;
    }

    public int getCurrent() {
        return current;
    }

    public boolean isDone() {
        return current == total;
    }
}
