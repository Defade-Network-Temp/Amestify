package net.defade.amestify.world;

import net.defade.amestify.loaders.anvil.RegionFile;
import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.chunk.pos.ChunkPos;
import net.defade.amestify.world.chunk.pos.RegionPos;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class World {
    private final Map<RegionPos, RegionFile> regionFiles = new HashMap<>();
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();

    public void addRegion(RegionFile regionFile) {
        regionFiles.put(regionFile.getRegionPos(), regionFile);
        for (Chunk chunk : regionFile.getChunks()) {
            if(chunk != null) chunks.put(chunk.getChunkPos(), chunk);
        }
    }

    public Collection<Chunk> getChunks() {
        return chunks.values();
    }

    public Collection<RegionFile> getRegions() {
        return regionFiles.values();
    }
}
