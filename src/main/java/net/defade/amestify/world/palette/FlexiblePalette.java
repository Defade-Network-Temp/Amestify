package net.defade.amestify.world.palette;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.defade.amestify.utils.Utils;

import java.util.Arrays;

/**
 * Palette able to take any value anywhere. May consume more memory than required.
 */
public final class FlexiblePalette implements Palette {
    // Specific to this palette type
    private final AdaptivePalette adaptivePalette;
    private byte bitsPerEntry;
    private int count;

    private long[] values;
    // palette index = value
    IntArrayList paletteToValueList;
    // value = palette index
    private Int2IntOpenHashMap valueToPaletteMap;

    FlexiblePalette(AdaptivePalette adaptivePalette, byte bitsPerEntry) {
        this.adaptivePalette = adaptivePalette;

        this.bitsPerEntry = bitsPerEntry;

        this.paletteToValueList = new IntArrayList(1);
        this.paletteToValueList.add(0);
        this.valueToPaletteMap = new Int2IntOpenHashMap(1);
        this.valueToPaletteMap.put(0, 0);
        this.valueToPaletteMap.defaultReturnValue(-1);

        final int valuesPerLong = 64 / bitsPerEntry;
        this.values = new long[(maxSize() + valuesPerLong - 1) / valuesPerLong];
    }

    public FlexiblePalette(AdaptivePalette adaptivePalette, byte bitsPerEntry, IntArrayList paletteToValueList, int count, long[] values) {
        this.adaptivePalette = adaptivePalette;
        this.bitsPerEntry = bitsPerEntry;
        this.paletteToValueList = paletteToValueList;

        this.valueToPaletteMap = new Int2IntOpenHashMap(paletteToValueList.size());
        this.valueToPaletteMap.defaultReturnValue(-1);
        for (int i = 0; i < paletteToValueList.size(); i++) {
            valueToPaletteMap.put(paletteToValueList.getInt(i), i);
        }

        this.count = count;
        this.values = values;
    }

    @Override
    public int get(int x, int y, int z) {
        final int bitsPerEntry = this.bitsPerEntry;
        final int sectionIndex = getSectionIndex(dimension(), x, y, z);
        final int valuesPerLong = 64 / bitsPerEntry;
        final int index = sectionIndex / valuesPerLong;
        final int bitIndex = (sectionIndex - index * valuesPerLong) * bitsPerEntry;
        final int value = (int) (values[index] >> bitIndex) & ((1 << bitsPerEntry) - 1);
        // Change to palette value and return
        return hasPalette() ? paletteToValueList.getInt(value) : value;
    }

    public void getAll(EntryConsumer consumer) {
        final long[] values = this.values;
        final int dimension = this.dimension();
        final int bitsPerEntry = this.bitsPerEntry;
        final int magicMask = (1 << bitsPerEntry) - 1;
        final int valuesPerLong = 64 / bitsPerEntry;
        final int size = maxSize();
        final int dimensionMinus = dimension - 1;
        final int[] ids = hasPalette() ? paletteToValueList.elements() : null;
        final int dimensionBitCount = Utils.bitsToRepresent(dimensionMinus);
        final int shiftedDimensionBitCount = dimensionBitCount << 1;
        for (int i = 0; i < values.length; i++) {
            final long value = values[i];
            final int startIndex = i * valuesPerLong;
            final int endIndex = Math.min(startIndex + valuesPerLong, size);
            for (int index = startIndex; index < endIndex; index++) {
                final int bitIndex = (index - startIndex) * bitsPerEntry;
                final int paletteIndex = (int) (value >> bitIndex & magicMask);
                final int y = index >> shiftedDimensionBitCount;
                final int z = index >> dimensionBitCount & dimensionMinus;
                final int x = index & dimensionMinus;
                final int result = ids != null ? ids[paletteIndex] : paletteIndex;
                consumer.accept(x, y, z, result);
            }
        }
    }

    @Override
    public void set(int x, int y, int z, int value) {
        value = getPaletteIndex(value);
        final int bitsPerEntry = this.bitsPerEntry;
        final long[] values = this.values;
        // Change to palette value
        final int valuesPerLong = 64 / bitsPerEntry;
        final int sectionIndex = getSectionIndex(dimension(), x, y, z);
        final int index = sectionIndex / valuesPerLong;
        final int bitIndex = (sectionIndex - index * valuesPerLong) * bitsPerEntry;

        final long block = values[index];
        final long clear = (1L << bitsPerEntry) - 1L;
        final long oldBlock = block >> bitIndex & clear;
        values[index] = block & ~(clear << bitIndex) | ((long) value << bitIndex);
        // Check if block count needs to be updated
        final boolean currentAir = oldBlock == 0;
        if (currentAir != (value == 0)) this.count += currentAir ? 1 : -1;
    }

    @Override
    public void fill(int value) {
        if (value == 0) {
            Arrays.fill(values, 0);
            this.count = 0;
            return;
        }
        value = getPaletteIndex(value);
        final int bitsPerEntry = this.bitsPerEntry;
        final int valuesPerLong = 64 / bitsPerEntry;
        final long[] values = this.values;
        long block = 0;
        for (int i = 0; i < valuesPerLong; i++)
            block |= (long) value << i * bitsPerEntry;
        Arrays.fill(values, block);
        this.count = maxSize();
    }

    @Override
    public int count() {
        return count;
    }

    public int bitsPerEntry() {
        return bitsPerEntry;
    }

    public int maxBitsPerEntry() {
        return adaptivePalette.maxBitsPerEntry;
    }

    @Override
    public int dimension() {
        return adaptivePalette.dimension();
    }

    public IntArrayList getPaletteToValueList() {
        return paletteToValueList;
    }

    public long[] values() {
        return values;
    }

    public void recalculateCount() {
        this.count = 0;

        getAll((x, y, z, value) -> {
            if (value != 0) count++;
        });
    }

    void resize(byte newBitsPerEntry) {
        newBitsPerEntry = newBitsPerEntry > maxBitsPerEntry() ? 15 : newBitsPerEntry;
        FlexiblePalette palette = new FlexiblePalette(adaptivePalette, newBitsPerEntry);
        palette.paletteToValueList = paletteToValueList;
        palette.valueToPaletteMap = valueToPaletteMap;
        getAll(palette::set);
        this.bitsPerEntry = palette.bitsPerEntry;
        this.values = palette.values;
        assert this.count == palette.count;
    }

    private int getPaletteIndex(int value) {
        if (!hasPalette()) return value;
        final int lastPaletteIndex = this.paletteToValueList.size();
        final byte bpe = this.bitsPerEntry;
        if (lastPaletteIndex >= maxPaletteSize(bpe)) {
            // Palette is full, must resize
            resize((byte) (bpe + 1));
            return getPaletteIndex(value);
        }
        final int lookup = valueToPaletteMap.putIfAbsent(value, lastPaletteIndex);
        if (lookup != -1) return lookup;
        this.paletteToValueList.add(value);
        assert lastPaletteIndex < maxPaletteSize(bpe);
        return lastPaletteIndex;
    }

    boolean hasPalette() {
        return bitsPerEntry <= maxBitsPerEntry();
    }

    static int getSectionIndex(int dimension, int x, int y, int z) {
        final int dimensionMask = dimension - 1;
        final int dimensionBitCount = Utils.bitsToRepresent(dimensionMask);
        return (y & dimensionMask) << (dimensionBitCount << 1) |
                (z & dimensionMask) << dimensionBitCount |
                (x & dimensionMask);
    }

    static int maxPaletteSize(int bitsPerEntry) {
        return 1 << bitsPerEntry;
    }

    interface EntryConsumer {
        void accept(int x, int y, int z, int value);
    }
}
