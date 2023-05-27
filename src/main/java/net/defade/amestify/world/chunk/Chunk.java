package net.defade.amestify.world.chunk;

import net.defade.amestify.world.biome.Biome;
import net.defade.amestify.world.pos.ChunkPos;
import net.defade.amestify.world.viewer.MapViewerWorld;

import java.util.ArrayList;
import java.util.List;

public abstract class Chunk {
    public static final int CHUNK_SECTION_SIZE = 16;

    protected final MapViewerWorld mapViewerWorld;

    protected final ChunkPos chunkPos;
    protected final int minY;
    protected final int maxY;

    protected final List<Section> sections = new ArrayList<>();
    protected final int[] heightMap = new int[256];

    protected Chunk(MapViewerWorld mapViewerWorld, ChunkPos chunkPos, int minY, int maxY) {
        this.mapViewerWorld = mapViewerWorld;
        this.chunkPos = chunkPos;

        this.minY = minY;
        this.maxY = maxY;

        int sectionsAmount = (-minY / CHUNK_SECTION_SIZE) + (maxY / CHUNK_SECTION_SIZE);

        for (int i = 0; i < sectionsAmount; i++) {
            sections.add(new Section());
        }
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    public Section getSection(int section) {
        return sections.get(section - (minY / CHUNK_SECTION_SIZE));
    }

    public List<Section> getSections() {
        return sections;
    }

    public boolean isEmpty() {
        for (Section section : getSections()) {
            if(section.getBlockPalette().isFilledPalette()) {
                if(section.getBlockPalette().get(0, 0, 0) != 0) return false;
            } else {
                return false;
            }
        }

        return true;
    }

    public int getBlockState(int x, int y, int z) {
        return getSection(y >> 4).getBlockPalette().get(x & 0xF, y & 0xF, z & 0xF);
    }

    public Biome getBiome(int x, int y, int z) {
        return mapViewerWorld.getBiomeById(getSection(y >> 4).getBiomePalette().get((x & 0xF) / 4, (y & 0xF) / 4, (z & 0xF) / 4));
    }

    public void setBiome(int x, int y, int z, Biome biome) {
        getSection(y >> 4).getBiomePalette().set((x & 0xF) / 4, (y & 0xF) / 4, (z & 0xF) / 4, biome.id());
    }

    public int getHighestBlockAt(int x, int z) {
        return heightMap[(x & 0xF) + ((z & 0xF) << 4)] + minY -1;
    }

}
