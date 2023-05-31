package net.defade.amestify.world.viewer;

import net.defade.amestify.graphics.rendering.Assets;
import net.defade.amestify.graphics.gui.renderer.RegionRenderer;
import net.defade.amestify.graphics.rendering.texture.block.BlockTexture;
import net.defade.amestify.world.biome.Biome;
import net.defade.amestify.world.chunk.Chunk;
import net.defade.amestify.world.pos.RegionPos;
import net.defade.amestify.world.RegionFile;

public class MapViewerRegion {
    public static final int TEXTURES_DEPTH = 3;

    private final RegionPos regionPos;
    private final Biome plainsBiome;

    private final int minY;

    private final BlockTexture[] blockTextures = new BlockTexture[512 * 512 * TEXTURES_DEPTH];
    private final Biome[] biomes = new Biome[128 * 128 * TEXTURES_DEPTH]; // Biomes in minecraft are stored in 4x4 blocks
    private final Biome[] updatedBiomes = new Biome[128 * 128]; // Biomes that have been updated. A null value means that the biome has not been updated so the original biome should be saved.
    private final boolean[] deletedChunks = new boolean[32 * 32]; // Chunks that have been deleted

    private final RegionRenderer regionRenderer = new RegionRenderer(this);

    public MapViewerRegion(RegionFile regionFile, Biome plainsBiome) {
        this.plainsBiome = plainsBiome;
        this.regionPos = regionFile.getRegionPos();
        this.minY = regionFile.getMinY();

        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                Chunk chunk = regionFile.getChunk(x, z);
                if(chunk != null && !chunk.isEmpty()) {
                    calculateTexturesForChunk(chunk);
                }
            }
        }

        regionRenderer.updateMesh();
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

    public Biome getModifiedBiome(int x, int z) {
        return updatedBiomes[(((x >> 2) << 7) + (z >> 2))];
    }

    public void setBiome(int x, int z, Biome biome) {
        int arrayIndex = (((((x & 0x1FF) >> 2) << 7) + ((z & 0x1FF) >> 2)));
        for (int layer = 0; layer < TEXTURES_DEPTH; layer++) {
            if(biomes[(arrayIndex * TEXTURES_DEPTH) + layer] == null) break;
            biomes[(arrayIndex * TEXTURES_DEPTH) + layer] = biome;
        }

        updatedBiomes[arrayIndex] = biome;
    }

    public void unregisterBiome(Biome biome) {
        for (int x = 0; x < 128; x++) {
            for (int z = 0; z < 128; z++) {
                int arrayIndex = (((x << 7) + z));
                if(updatedBiomes[arrayIndex] == biome) {
                    updatedBiomes[arrayIndex] = plainsBiome;
                }

                arrayIndex = (arrayIndex * TEXTURES_DEPTH);
                for (int layer = 0; layer < TEXTURES_DEPTH; layer++) {
                    Biome currentBiome = biomes[arrayIndex + layer];
                    if(currentBiome == null) break;
                    if(currentBiome == biome) {
                        biomes[arrayIndex + layer] = plainsBiome;
                    }
                }
            }
        }

        regionRenderer.updateMesh();
    }

    public RegionRenderer getRenderer() {
        return regionRenderer;
    }

    public boolean isChunkDeleted(int x, int z) {
        return deletedChunks[RegionFile.getChunkIndex(x, z)];
    }

    public void setChunkDeleted(int x, int z, boolean deleted) {
        deletedChunks[RegionFile.getChunkIndex(x, z)] = deleted;
    }

    private void calculateTexturesForChunk(Chunk chunk) {
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

                    Biome biome = chunk.getBiome(x, y, z);
                    biomes[((((normalizedX / 4) << 7) + (normalizedZ / 4)) * TEXTURES_DEPTH) + layer] = biome;

                    if(!texture.isTranslucent()) break;
                    layer++;
                    y--;
                    if (y <= minY) break;
                }
            }
        }
    }
}
