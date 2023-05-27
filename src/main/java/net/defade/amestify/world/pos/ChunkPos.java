package net.defade.amestify.world.pos;

public record ChunkPos(int x, int z) {
    public long getChunkIndex() {
        return (((long) x) << 32) | (z & 0xffffffffL);
    }

    public ChunkPos add(int x, int z) {
        return new ChunkPos(x() + x, z() + z);
    }
}
