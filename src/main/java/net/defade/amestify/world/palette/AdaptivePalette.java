package net.defade.amestify.world.palette;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.defade.amestify.utils.Utils;

public class AdaptivePalette implements Palette {
    protected final byte dimension, defaultBitsPerEntry, maxBitsPerEntry;
    private Palette palette;

    public AdaptivePalette(byte dimension, byte maxBitsPerEntry, byte bitsPerEntry) {
        this(dimension, maxBitsPerEntry, bitsPerEntry, new FilledPalette(dimension, (short) 0));
    }

    public AdaptivePalette(byte dimension, byte maxBitsPerEntry, byte bitsPerEntry, Palette palette) {
        validateDimension(dimension);
        this.dimension = dimension;
        this.maxBitsPerEntry = maxBitsPerEntry;
        this.defaultBitsPerEntry = bitsPerEntry;
        this.palette = palette;
    }

    @Override
    public int get(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0) {
            throw new IllegalArgumentException("Coordinates must be positive");
        }
        return palette.get(x, y, z);
    }

    @Override
    public void set(int x, int y, int z, int value) {
        if (x < 0 || y < 0 || z < 0) {
            throw new IllegalArgumentException("Coordinates must be positive");
        }
        flexiblePalette().set(x, y, z, value);
    }

    @Override
    public void fill(int value) {
        this.palette = new FilledPalette(dimension, value);
    }

    @Override
    public int count() {
        return palette.count();
    }

    @Override
    public int dimension() {
        return dimension;
    }

    public boolean isFilledPalette() {
        return palette instanceof FilledPalette;
    }

    public FlexiblePalette getAsFlexiblePalette() {
        if(palette instanceof FlexiblePalette) {
            return (FlexiblePalette) palette;
        } else {
            FlexiblePalette flexiblePalette = new FlexiblePalette(this, defaultBitsPerEntry);
            flexiblePalette.fill(palette.get(0, 0, 0)); // This is a filled palette, so all the values are the same.

            return flexiblePalette;
        }
    }

    public void optimize() {
        this.palette = optimizedPalette();
    }

    public void setPalette(Palette palette) {
        this.palette = palette;
        optimize();
    }

    Palette optimizedPalette() {
        var currentPalette = this.palette;
        if (currentPalette instanceof FlexiblePalette flexiblePalette) {
            final int count = flexiblePalette.count();
            if (count == 0) {
                return new FilledPalette(dimension, (short) 0);
            } else {
                // Find all entries and compress the palette
                IntSet entries = new IntOpenHashSet(flexiblePalette.paletteToValueList.size());
                flexiblePalette.getAll((x, y, z, value) -> entries.add(value));
                final int currentBitsPerEntry = flexiblePalette.bitsPerEntry();
                final int bitsPerEntry;
                if (entries.size() == 1) {
                    return new FilledPalette(dimension, entries.iterator().nextInt());
                } else if (currentBitsPerEntry > defaultBitsPerEntry &&
                        (bitsPerEntry = Utils.bitsToRepresent(entries.size() - 1)) < currentBitsPerEntry) {
                    flexiblePalette.resize((byte) bitsPerEntry);
                    return flexiblePalette;
                }
            }
        }
        return currentPalette;
    }

    Palette flexiblePalette() {
        Palette currentPalette = this.palette;
        if (currentPalette instanceof FilledPalette filledPalette) {
            currentPalette = new FlexiblePalette(this, defaultBitsPerEntry);
            currentPalette.fill(filledPalette.getValue());
            this.palette = currentPalette;
        }
        return currentPalette;
    }

    private static void validateDimension(int dimension) {
        if (dimension <= 1 || (dimension & dimension - 1) != 0)
            throw new IllegalArgumentException("Dimension must be a positive power of 2");
    }
}
