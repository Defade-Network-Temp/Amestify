package net.defade.amestify.world.loaders;

import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.chunk.pos.RegionPos;

public interface RegionFile {
    RegionPos getRegionPos();

    Chunk getChunk(int x, int z);

    int getMinY();
}
