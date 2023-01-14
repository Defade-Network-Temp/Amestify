package net.defade.amestify.world;

import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.chunk.pos.ChunkPos;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class World {
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();

    public void addChunk(Chunk chunk) {
        chunks.put(chunk.getChunkPos(), chunk);
    }


    public Collection<Chunk> getChunks() {
        return chunks.values();
    }
}
