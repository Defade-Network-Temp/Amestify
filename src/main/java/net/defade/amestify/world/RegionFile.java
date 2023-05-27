package net.defade.amestify.world;

import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.pos.RegionPos;

public interface RegionFile {
    RegionPos getRegionPos();

    Chunk getChunk(int x, int z);

    int getMinY();
}
