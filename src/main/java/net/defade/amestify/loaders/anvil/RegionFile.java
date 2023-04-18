package net.defade.amestify.loaders.anvil;

import net.defade.amestify.graphics.Assets;
import net.defade.amestify.graphics.texture.block.BlockTexture;
import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.world.World;
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

public class RegionFile {
    public static final int TEXTURES_DEPTH = 3;

    private final RegionPos regionPos;

    private final int minY;
    private final BlockTexture[] blockTextures = new BlockTexture[512 * 512 * TEXTURES_DEPTH];
    private final Biome[] biomes = new Biome[128 * 128 * TEXTURES_DEPTH]; // Biomes in minecraft are stored in 4x4 blocks

    public RegionFile(ProgressTracker progressTracker, World world, Path regionFilePath, RegionPos regionPos, int minY, int maxY) throws IOException {
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
                    world,
                    getChunkPosFromIndex(i).add(regionPos.x() * 32, regionPos.z() * 32),
                    minY, maxY, // TODO minY and maxY
                    compressionType, chunkInputStream
            );
            if(progressTracker != null) progressTracker.increment();
            if(chunk.isEmpty()) {
                continue;
            }

            calculateTexturesForChunk(chunk);
        }

        regionFile.close();
    }

    public RegionPos getRegionPos() {
        return regionPos;
    }

    public BlockTexture getMapViewerTextureLayer(int x, int z, int layer) {
        return blockTextures[(((x << 9) + z) * TEXTURES_DEPTH) + layer];
    }

    public Biome getMapViewerBiome(int x, int z, int layer) {
        return biomes[((((x / 4) << 7) + (z / 4)) * TEXTURES_DEPTH) + layer];
    }

    public void calculateTexturesForChunk(Chunk chunk) {
        for (int x = chunk.getChunkPos().x() * 16; x < chunk.getChunkPos().x() * 16 + 16; x++) {
            for (int z = chunk.getChunkPos().z() * 16; z < chunk.getChunkPos().z() * 16 + 16; z++) {

                int y = chunk.getHighestBlockAt(x, z);
                if(y <= minY) {
                    continue;
                }

                int layer = 0;
                BlockTexture lastTexture = null;
                while (layer < TEXTURES_DEPTH) {
                    BlockTexture texture = Assets.BLOCK_SHEET.getBlockTexture(chunk.getBlockState(x, y, z));
                    while(texture.isIgnored() || texture.equals(lastTexture)) {
                        y--;
                        if(y <= minY) break;
                        texture = Assets.BLOCK_SHEET.getBlockTexture(chunk.getBlockState(x, y, z));
                    }

                    lastTexture = texture;
                    int normalizedX = x & 0x1FF;
                    int normalizedZ = z & 0x1FF;
                    blockTextures[(((normalizedX << 9) + normalizedZ) * TEXTURES_DEPTH) + layer] = texture;
                    biomes[((((normalizedX / 4) << 7) + (normalizedZ / 4)) * TEXTURES_DEPTH) + layer] = chunk.getBiome(x, y, z);

                    if(!texture.isTranslucent()) break;
                    layer++;
                    y--;
                    if (y <= minY) break;
                }
            }
        }
    }

    public static int getChunkIndex(int x, int z) {
        return (x & 0x1F) + ((z & 0x1F) << 5);
    }

    public static ChunkPos getChunkPosFromIndex(int index) {
        return new ChunkPos(index & 0x1f, index >> 5);
    }
}
