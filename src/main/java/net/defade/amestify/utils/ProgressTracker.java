package net.defade.amestify.utils;

public class ProgressTracker {
    private long total = 0;
    private long current = 0;
    private String message = null;

    public void reset(long total) {
        this.total = total;
        this.current = 0;
    }

    public void increment() {
        current++;
    }

    public void increment(int amount) {
        current += amount;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public float getProgress() {
        return (float) current / total;
    }

    public long getTotal() {
        return total;
    }

    public long getCurrent() {
        return current;
    }

    public boolean isDone() {
        return current == total;
    }
}
