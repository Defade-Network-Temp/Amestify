package net.defade.amestify.graphics.gui.map;

import net.defade.amestify.graphics.texture.block.BlockTexture;
import net.defade.amestify.loaders.anvil.RegionFile;
import net.defade.amestify.world.biome.Biome;
import org.lwjgl.opengl.GL46;

import static org.lwjgl.opengl.GL46.*;

public class RegionRenderer {
    private static final int POS_SIZE = 2;
    private static final int COLOR_SIZE = 1;
    private static final int TEX_COORDS_SIZE = 2;
    private static final int TEX_ID_SIZE = 1;

    private static final int POS_OFFSET = 0;
    private static final int COLOR_OFFSET = POS_OFFSET + POS_SIZE * Float.BYTES;
    private static final int TEX_COORDS_OFFSET = COLOR_OFFSET + COLOR_SIZE * Float.BYTES;
    private static final int TEX_ID_OFFSET = TEX_COORDS_OFFSET + TEX_COORDS_SIZE * Float.BYTES;

    private static final int VERTEX_SIZE = POS_SIZE + COLOR_SIZE + TEX_COORDS_SIZE + TEX_ID_SIZE;
    private static final int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES;

    private static final int[] UV_POSITIONS = new int[] {
            1, 1,
            1, 0,
            0, 0,
            0, 1
    };

    private final RegionFile regionFile;
    private int squares = 0;
    private float[] vertices = new float[32 * 32 * 16 * 16 * 4 * VERTEX_SIZE];

    private int vaoID, vboID, eboID;

    public RegionRenderer(RegionFile regionFile) {
        this.regionFile = regionFile;
        for (int layer = RegionFile.TEXTURES_DEPTH - 1; layer >= 0; layer--) {
            generateMeshForLayer(layer);
        }

        float[] vertices = new float[squares * 4 * VERTEX_SIZE];
        System.arraycopy(this.vertices, 0, vertices, 0, vertices.length);
        this.vertices = vertices;
    }

    public void init() {
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, generateIndices(), GL_STATIC_DRAW);

        GL46.glVertexAttribPointer(0, POS_SIZE, GL46.GL_FLOAT, false, VERTEX_SIZE_BYTES, POS_OFFSET);
        GL46.glEnableVertexAttribArray(0);

        GL46.glVertexAttribPointer(1, COLOR_SIZE, GL46.GL_FLOAT, false, VERTEX_SIZE_BYTES, COLOR_OFFSET);
        GL46.glEnableVertexAttribArray(1);

        GL46.glVertexAttribPointer(2, TEX_COORDS_SIZE, GL46.GL_FLOAT, false, VERTEX_SIZE_BYTES, TEX_COORDS_OFFSET);
        GL46.glEnableVertexAttribArray(2);

        GL46.glVertexAttribPointer(3, TEX_ID_SIZE, GL46.GL_FLOAT, false, VERTEX_SIZE_BYTES, TEX_ID_OFFSET);
        GL46.glEnableVertexAttribArray(3);
        vertices = null;
    }

    public void stop() {
        GL46.glDeleteVertexArrays(vaoID);
        GL46.glDeleteBuffers(vboID);
        GL46.glDeleteBuffers(eboID);
        GL46.glDisableVertexAttribArray(0);
        GL46.glDisableVertexAttribArray(1);
        GL46.glDisableVertexAttribArray(2);
        GL46.glDisableVertexAttribArray(3);
    }

    public void render() {
        glBindBuffer(GL_ARRAY_BUFFER, vboID);

        glBindVertexArray(vaoID);
        GL46.glEnableVertexAttribArray(0);
        GL46.glEnableVertexAttribArray(1);
        GL46.glEnableVertexAttribArray(2);
        GL46.glEnableVertexAttribArray(3);

        glDrawElements(GL_TRIANGLES, squares * 6, GL_UNSIGNED_INT, 0);

        GL46.glDisableVertexAttribArray(0);
        GL46.glDisableVertexAttribArray(1);
        GL46.glDisableVertexAttribArray(2);
        GL46.glDisableVertexAttribArray(3);
        glBindVertexArray(0);
    }

    private void generateMeshForLayer(int layer) {
        boolean[] isMeshed = new boolean[32 * 32 * 16 * 16];
        for (int x = 0; x < 512; x++) {
            for (int z = 0; z < 512; z++) {
                int arrayIndex = (x << 9) + z;

                if(isMeshed[arrayIndex]) {
                    continue;
                }

                BlockTexture texture = getTextureLayer(x, z, layer);
                Biome biome = getBiome(x, z, layer);
                if(texture == null || biome == null) continue; // Invisible block

                isMeshed[arrayIndex] = true;
                int endX = x + 1;

                while (endX < 512 && texture.equals(getTextureLayer(endX, z, layer)) && biome.equals(getBiome(endX, z, layer))) {
                    isMeshed[(endX << 9) + z] = true;
                    endX++;
                }
                endX -= 1;

                int endZ = Integer.MAX_VALUE;
                for (int deltaX = x; deltaX <= endX; deltaX++) {
                    int foundZ = z;
                    for (int deltaZ = z + 1; deltaZ < 512; deltaZ++) {
                        int index = (deltaX << 9) + deltaZ;
                        if(isMeshed[index]) break;
                        if(texture.equals(getTextureLayer(deltaX, deltaZ, layer)) && biome.equals(getBiome(deltaX, deltaZ, layer))) {
                            foundZ = deltaZ;
                        } else {
                            break;
                        }
                    }

                    if(foundZ < endZ) {
                        endZ = foundZ;
                    }
                }

                for (int deltaX = x; deltaX <= endX; deltaX++) {
                    for (int deltaZ = z; deltaZ <= endZ; deltaZ++) {
                        isMeshed[(deltaX << 9) + deltaZ] = true;
                    }
                }

                addBlockAtRelativePos(x, z, endX, endZ, texture, biome);
            }
        }
    }

    private BlockTexture getTextureLayer(int x, int z, int layer) {
        return regionFile.getMapViewerTextureLayer(x, z, layer);
    }

    private Biome getBiome(int x, int z, int layer) {
        return regionFile.getMapViewerBiome(x, z, layer);
    }

    private void addBlockAtRelativePos(int relativeStartX, int relativeStartZ, int relativeEndX, int relativeEndZ, BlockTexture blockTexture, Biome biome) {
        relativeStartX = regionFile.getRegionPos().x() * 512 + relativeStartX;
        relativeStartZ = regionFile.getRegionPos().z() * 512 + relativeStartZ;
        relativeEndX = regionFile.getRegionPos().x() * 512 + relativeEndX;
        relativeEndZ = regionFile.getRegionPos().z() * 512 + relativeEndZ;

        int textureLayer = blockTexture.textureLayer();

        int color = 0xFFFFFFFF;
        if(blockTexture.isWater()) {
            color = biome.effects().waterColor();
        } else if(blockTexture.isGrass()) {
            color = biome.effects().grassColor();
        } else if(blockTexture.isFoliage()) {
            color = biome.effects().foliageColor();
        }

        float rgb = color ^ 0xFF000000; // Remove the alpha channel

        int xPos = relativeStartX * 16;
        int yPos = relativeStartZ * 16;

        int offset = squares * 4 * VERTEX_SIZE;

        float xAdd = 1;
        float yAdd = 1;
        for (int i = 0; i < 4; i++) {
            if(i == 1) {
                yAdd = 0;
            } else if(i == 2) {
                xAdd = 0;
            } else if(i == 3) {
                yAdd = 1;
            }

            vertices[offset] = xPos + (xAdd * ((relativeEndX - relativeStartX) + 1) * 16);
            vertices[offset + 1] = yPos + (yAdd * ((relativeEndZ - relativeStartZ) + 1) * 16);

            vertices[offset + 2] = rgb;

            vertices[offset + 3] = UV_POSITIONS[i * 2] * ((relativeEndX - relativeStartX) + 1);
            vertices[offset + 4] = UV_POSITIONS[i * 2 + 1] * ((relativeEndZ - relativeStartZ) + 1);

            vertices[offset + 5] = textureLayer;

            offset += VERTEX_SIZE;
        }

        squares++;
    }

    private int[] generateIndices() {
        int[] elements = new int[6 * squares]; // 6 indices per quad (4 per triangle)
        for (int index = 0; index < squares; index++) {
            int offsetArrayIndex = 6 * index;
            int offset = 4 * index;

            elements[offsetArrayIndex] = offset + 3;
            elements[offsetArrayIndex + 1] = offset + 2;
            elements[offsetArrayIndex + 2] = offset;

            elements[offsetArrayIndex + 3] = offset;
            elements[offsetArrayIndex + 4] = offset + 2;
            elements[offsetArrayIndex + 5] = offset + 1;
        }

        return elements;
    }
}
