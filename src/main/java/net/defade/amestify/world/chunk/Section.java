package net.defade.amestify.world.chunk;

import net.defade.amestify.world.chunk.palette.AdaptivePalette;

public class Section {
    private final AdaptivePalette blockPalette;
    private final AdaptivePalette biomePalette;
    private byte[] skyLight;
    private byte[] blockLight;

    public Section(AdaptivePalette blockPalette, AdaptivePalette biomePalette, byte[] skyLight, byte[] blockLight) {
        this.blockPalette = blockPalette;
        this.biomePalette = biomePalette;
        this.skyLight = skyLight;
        this.blockLight = blockLight;
    }

    public Section() {
        this(
                new AdaptivePalette((byte) 16, (byte) 8, (byte) 4),
                new AdaptivePalette((byte) 4, (byte) 3, (byte) 1),
                new byte[0],
                new byte[0]
        );
    }

    public AdaptivePalette getBlockPalette() {
        return blockPalette;
    }

    public AdaptivePalette getBiomePalette() {
        return biomePalette;
    }

    public byte[] getSkyLight() {
        return skyLight;
    }

    public byte[] getBlockLight() {
        return blockLight;
    }

    public void setSkyLight(byte[] skyLight) {
        this.skyLight = skyLight;
    }

    public void setBlockLight(byte[] blockLight) {
        this.blockLight = blockLight;
    }
}
