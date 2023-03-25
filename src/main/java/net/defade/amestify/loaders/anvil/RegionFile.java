package net.defade.amestify.loaders.anvil;

import net.defade.amestify.graphics.Assets;
import net.defade.amestify.graphics.texture.block.BlockTexture;
import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.world.biome.Biome;
import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.chunk.pos.ChunkPos;
import net.defade.amestify.world.chunk.pos.RegionPos;
import net.querz.mca.CompressionType;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class RegionFile {
    public static final int TEXTURES_DEPTH = 3;

    private final RegionPos regionPos;
    private final Chunk[] chunks = new Chunk[1024];

    private final int minY;
    private final BlockTexture[] blockTextures = new BlockTexture[512 * 512 * TEXTURES_DEPTH];
    private final Biome[] biomes = new Biome[512 * 512 * TEXTURES_DEPTH];

    public RegionFile(ProgressTracker progressTracker, Path regionFilePath, RegionPos regionPos, int minY, int maxY) throws IOException {
        this.regionPos = regionPos;
        this.minY = minY;

        RandomAccessFile regionFile = new RandomAccessFile(regionFilePath.toFile(), "r");

        for (int i = 0; i < 1024; i++) {
            regionFile.seek(i * 4);

            int offset = regionFile.read() << 16;
            offset |= (regionFile.read() & 0xFF) << 8;
            offset |= regionFile.read() & 0xFF;

            if (regionFile.readByte() == 0) {
                if(progressTracker != null) progressTracker.increment();
                continue;
            }

            regionFile.seek(4096L * offset + 4); // +4: skip data size

            byte compressionScheme = regionFile.readByte();
            InputStream chunkInputStream = new FileInputStream(regionFile.getFD());
            CompressionType compressionType = CompressionType.getFromID(compressionScheme);

            Chunk chunk = new Chunk(
                    getChunkPosFromIndex(i).add(regionPos.x() * 32, regionPos.z() * 32),
                    minY, maxY, // TODO minY and maxY
                    compressionType, chunkInputStream
            );
            if(progressTracker != null) progressTracker.increment();
            if(chunk.isEmpty()) {
                continue;
            }

            chunks[i] = chunk;
        }

        regionFile.close();
    }

    public RegionPos getRegionPos() {
        return regionPos;
    }

    public Chunk getChunk(int x, int z) {
        return chunks[getChunkIndex(x >> 4, z >> 4)];
    }

    public List<Chunk> getChunks() {
        return Arrays.asList(chunks);
    }

    public int getBlockState(int x, int y, int z) {
        Chunk chunk = getChunk(x, z);
        if(chunk == null) return 0;
        return chunk.getBlockState(x, y, z);
    }

    public Biome getBiome(int x, int y, int z) {
        Chunk chunk = getChunk(x, z);
        if(chunk == null) return null;
        return chunk.getBiome(x, y, z);
    }

    public int getHighestBlockAt(int x, int z) {
        Chunk chunk = getChunk(x, z);
        if(chunk == null) return minY;
        return chunk.getHighestBlockAt(x, z);
    }

    public BlockTexture getMapViewerTextureLayer(int x, int z, int layer) {
        return blockTextures[(((x << 9) + z) * TEXTURES_DEPTH) + layer];
    }

    public Biome getMapViewerBiome(int x, int z, int layer) {
        return biomes[(((x << 9) + z) * TEXTURES_DEPTH) + layer];
    }

    public void calculateTextures() {
        for (int x = 0; x < 512; x++) {
            for (int z = 0; z < 512; z++) {
                int arrayIndex = ((x << 9) + z) * TEXTURES_DEPTH;
                int y = getHighestBlockAt(x, z);
                if(y <= minY) {
                    continue;
                }

                int blockState = getBlockState(x, y, z);
                BlockTexture texture = Assets.BLOCK_SHEET.getBlockTexture(blockState);

                for (int layer = 0; layer < TEXTURES_DEPTH; layer++) {
                    BlockTexture lastTexture = layer > 0 ? blockTextures[arrayIndex + layer - 1] : null;
                    while (texture.isIgnored() || texture.equals(lastTexture)) {
                        y--;
                        if(y <= minY) {
                            break;
                        }

                        blockState = getBlockState(x, y, z);
                        texture = Assets.BLOCK_SHEET.getBlockTexture(blockState);
                    }

                    blockTextures[arrayIndex + layer] = texture;
                    biomes[arrayIndex + layer] = getChunk(x, z).getBiome(x, y,z);
                    if(!texture.isTranslucent()) break;

                    y--;
                    if(y <= minY) {
                        break;
                    }
                    blockState = getBlockState(x, y, z);
                    texture = Assets.BLOCK_SHEET.getBlockTexture(blockState);
                }
            }
        }

        for (Chunk chunk : chunks) {
            if(chunk != null) chunk.clearHeightMap();
        }
    }

    public static int getChunkIndex(int x, int z) {
        return (x & 0x1F) + ((z & 0x1F) << 5);
    }

    public static ChunkPos getChunkPosFromIndex(int index) {
        return new ChunkPos(index & 0x1f, index >> 5);
    }
}
