package net.defade.amestify.world.chunk.palette;

public interface Palette {
    int get(int x, int y, int z);

    void set(int x, int y, int z, int value);

    void fill(int value);

    /**
     * Returns the number of entries in this palette.
     */
    int count();

    int dimension();

    /**
     * Returns the maximum number of entries in this palette.
     */
    default int maxSize() {
        final int dimension = dimension();
        return dimension * dimension * dimension;
    }
}
