package net.defade.amestify.world.loaders.amethyst;

import net.defade.amestify.world.RegionFile;
import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.pos.RegionPos;
import java.util.List;

public class AmethystRegionFile implements RegionFile {
    private final RegionPos regionPos;
    private final int minY;

    private final Chunk[] chunks = new Chunk[1024];

    public AmethystRegionFile(RegionPos regionPos, int minY, List<Chunk> chunks) {
        this.regionPos = regionPos;
        this.minY = minY;

        for (Chunk chunk : chunks) {
            this.chunks[RegionFile.getChunkIndex(chunk.getChunkPos().x(), chunk.getChunkPos().z())] = chunk;
        }
    }

    @Override
    public RegionPos getRegionPos() {
        return regionPos;
    }

    @Override
    public Chunk getChunk(int x, int z) {
        return chunks[RegionFile.getChunkIndex(x, z)];
    }

    @Override
    public int getMinY() {
        return minY;
    }
}
