package net.defade.amestify.world.chunk.palette;

public class FilledPalette implements Palette {
    private final byte dimension;
    private final int value;

    public FilledPalette(byte dimension, int value) {
        this.dimension = dimension;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public int get(int x, int y, int z) {
        return value;
    }

    @Override
    public void set(int x, int y, int z, int value) {
        throw new UnsupportedOperationException("Cannot set value in a FilledPalette.");
    }

    @Override
    public void fill(int value) {
        throw new UnsupportedOperationException("Cannot change values in a FilledPalette.");
    }

    @Override
    public int count() {
        return value == 0 ? 0 : maxSize();
    }


    @Override
    public int dimension() {
        return dimension;
    }
}
