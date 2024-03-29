package net.defade.amestify.graphics.gui.renderer;

import net.defade.amestify.graphics.rendering.texture.block.BlockTexture;
import net.defade.amestify.world.viewer.MapViewerRegion;
import net.defade.amestify.world.biome.Biome;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.opengl.GL46.*;

public class RegionRenderer {
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors() - 4), runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    });

    private static final int POS_SIZE = 2;
    private static final int COLOR_SIZE = 1;
    private static final int BIOME_ID = 1;
    private static final int TEX_COORDS_SIZE = 2;
    private static final int TEX_ID_SIZE = 1;

    private static final int POS_OFFSET = 0;
    private static final int COLOR_OFFSET = POS_OFFSET + POS_SIZE * Float.BYTES;
    private static final int BIOME_ID_OFFSET = COLOR_OFFSET + COLOR_SIZE * Float.BYTES;
    private static final int TEX_COORDS_OFFSET = BIOME_ID_OFFSET + BIOME_ID * Float.BYTES;
    private static final int TEX_ID_OFFSET = TEX_COORDS_OFFSET + TEX_COORDS_SIZE * Float.BYTES;

    private static final int VERTEX_SIZE = POS_SIZE + COLOR_SIZE + BIOME_ID + TEX_COORDS_SIZE + TEX_ID_SIZE;
    private static final int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES;

    private static final int[] UV_POSITIONS = new int[] {
            1, 1,
            1, 0,
            0, 0,
            0, 1
    };

    private final MapViewerRegion mapViewerRegion;
    private int squares = 0;
    private float[] vertices;

    private int vaoID, vboID, eboID;

    private boolean buffersInitialized = false;
    private boolean isDirty = false;

    private final AtomicBoolean isBuildingMesh = new AtomicBoolean(false);
    private boolean outdatedMeshData = false; // When the updateMesh() method is called but the mesh is already being built

    public RegionRenderer(MapViewerRegion mapViewerRegion) {
        this.mapViewerRegion = mapViewerRegion;
    }

    public void updateMesh() {
        if (isBuildingMesh.get()) {
            outdatedMeshData = true;
            return;
        }
        isBuildingMesh.set(true);

        THREAD_POOL.submit(() -> {
            float[] vertices = new float[32 * 32 * 16 * 16 * 4 * VERTEX_SIZE];

            int squaresToRender = 0;
            for (int layer = MapViewerRegion.TEXTURES_DEPTH - 1; layer >= 0; layer--) {
                squaresToRender += generateMeshForLayer(vertices, layer, squaresToRender);
            }

            float[] newVertices = new float[squaresToRender * 4 * VERTEX_SIZE];
            System.arraycopy(vertices, 0, newVertices, 0, newVertices.length);

            this.vertices = newVertices;
            this.squares = squaresToRender;

            isDirty = true;
            isBuildingMesh.set(false);
        });
    }

    private void initBuffers() {
        vboID = glCreateBuffers();
        eboID = glCreateBuffers();
        vaoID = glCreateVertexArrays();

        glVertexArrayVertexBuffer(vaoID, 0, vboID, 0, VERTEX_SIZE_BYTES);
        glVertexArrayElementBuffer(vaoID, eboID);

        glEnableVertexArrayAttrib(vaoID, 0);
        glEnableVertexArrayAttrib(vaoID, 1);
        glEnableVertexArrayAttrib(vaoID, 2);
        glEnableVertexArrayAttrib(vaoID, 3);
        glEnableVertexArrayAttrib(vaoID, 4);

        glVertexArrayAttribFormat(vaoID, 0, POS_SIZE, GL_FLOAT, false, POS_OFFSET);
        glVertexArrayAttribFormat(vaoID, 1, COLOR_SIZE, GL_FLOAT, false, COLOR_OFFSET);
        glVertexArrayAttribFormat(vaoID, 2, BIOME_ID, GL_FLOAT, false, BIOME_ID_OFFSET);
        glVertexArrayAttribFormat(vaoID, 3, TEX_COORDS_SIZE, GL_FLOAT, false, TEX_COORDS_OFFSET);
        glVertexArrayAttribFormat(vaoID, 4, TEX_ID_SIZE, GL_FLOAT, false, TEX_ID_OFFSET);

        glVertexArrayAttribBinding(vaoID, 0, 0);
        glVertexArrayAttribBinding(vaoID, 1, 0);
        glVertexArrayAttribBinding(vaoID, 2, 0);
        glVertexArrayAttribBinding(vaoID, 3, 0);
        glVertexArrayAttribBinding(vaoID, 4, 0);
    }

    private void updateBuffers() {
        glNamedBufferData(vboID, vertices, GL_STATIC_DRAW);
        glNamedBufferData(eboID, generateIndices(), GL_STATIC_DRAW);

        vertices = null;
    }

    public void stop() {
        glDeleteBuffers(vboID);
        glDeleteBuffers(eboID);
        glDeleteVertexArrays(vaoID);
    }

    public void render() {
        if(!buffersInitialized) {
            initBuffers();
            buffersInitialized = true;
        }

        if(isDirty) {
            updateBuffers();
            isDirty = false;
        }

        if(!isBuildingMesh.get() && outdatedMeshData) {
            updateMesh();
            outdatedMeshData = false;
        }

        glBindVertexArray(vaoID);
        glDrawElements(GL_TRIANGLES, squares * 6, GL_UNSIGNED_INT, 0);
    }

    private int generateMeshForLayer(float[] vertices, int layer, int renderedSquares) {
        int createdSquares = 0;
        boolean[] isMeshed = new boolean[32 * 32 * 16 * 16];
        for (int x = 0; x < 512; x++) {
            for (int z = 0; z < 512; z++) {
                int arrayIndex = (x << 9) + z;

                if(isMeshed[arrayIndex]) { // The block is already meshed, skip it
                    continue;
                }

                BlockTexture texture = getTextureLayer(x, z, layer);
                Biome biome = getBiome(x, z, layer);
                boolean isDeleted = mapViewerRegion.isChunkDeleted(x >> 4, z >> 4);
                if(texture == null || texture.isIgnored() || biome == null) continue; // Invisible block

                isMeshed[arrayIndex] = true; // We're going to mesh this block
                int endX = x + 1; // We just meshed the block at x, z, so we can start at x + 1

                // Find the max x where the blocks are the same starting from x, z
                while (endX < 512 &&
                        texture.equals(getTextureLayer(endX, z, layer)) &&
                        biome.equals(getBiome(endX, z, layer)) &&
                        isDeleted == mapViewerRegion.isChunkDeleted(endX >> 4, z >> 4)) {
                    isMeshed[(endX << 9) + z] = true;
                    endX++;
                }
                endX -= 1; // Since it's a while loop, endX will be 1 higher than the last block that was the same

                // We're going to try to find the highest possible z value where blocks are all the same
                int endZ = 512;
                for (int deltaX = x; deltaX <= endX; deltaX++) { // For each block on the x-axis we found
                    int foundZ = z;
                    for (int deltaZ = z + 1; deltaZ < endZ; deltaZ++) { // Loop trough every z block on the x-axis
                        int index = (deltaX << 9) + deltaZ;
                        if(isMeshed[index]) break; // If we already meshed this block, we stop searching
                        if(texture.equals(getTextureLayer(deltaX, deltaZ, layer)) &&
                                biome.equals(getBiome(deltaX, deltaZ, layer)) &&
                                isDeleted == mapViewerRegion.isChunkDeleted(deltaX >> 4, deltaZ >> 4)) {
                            foundZ = deltaZ; // If the block is the same, we update the foundZ value
                        } else {
                            break; // They're not the same, we can't continue searching
                        }
                    }

                    if(foundZ < endZ) {
                        endZ = foundZ; // If we found a lower z value, we update the endZ value
                    }
                }

                for (int deltaX = x; deltaX <= endX; deltaX++) {
                    for (int deltaZ = z; deltaZ <= endZ; deltaZ++) {
                        isMeshed[(deltaX << 9) + deltaZ] = true;
                    }
                }

                addBlockAtRelativePos(vertices, renderedSquares + createdSquares, x, z, endX, endZ, texture, biome, isDeleted);
                createdSquares++;
            }
        }

        return createdSquares;
    }

    private BlockTexture getTextureLayer(int x, int z, int layer) {
        return mapViewerRegion.getMapViewerTextureLayer(x, z, layer);
    }

    private Biome getBiome(int x, int z, int layer) {
        return mapViewerRegion.getMapViewerBiome(x, z, layer);
    }

    private void addBlockAtRelativePos(float[] vertices, int renderedSquares, int relativeStartX, int relativeStartZ,
                                       int relativeEndX, int relativeEndZ, BlockTexture blockTexture, Biome biome,
                                       boolean isDeleted) {
        relativeStartX = mapViewerRegion.getRegionPos().x() * 512 + relativeStartX;
        relativeStartZ = mapViewerRegion.getRegionPos().z() * 512 + relativeStartZ;
        relativeEndX = mapViewerRegion.getRegionPos().x() * 512 + relativeEndX;
        relativeEndZ = mapViewerRegion.getRegionPos().z() * 512 + relativeEndZ;

        int textureLayer = blockTexture.textureLayer();

        int color = 0xFFFFFFFF;
        if(blockTexture.isWater()) {
            color = biome.effects().waterColor();
        } else if(blockTexture.isGrass()) {
            color = biome.effects().grassColor();
        } else if(blockTexture.isFoliage()) {
            color = biome.effects().foliageColor();
        }

        int xPos = relativeStartX * 16;
        int yPos = relativeStartZ * 16;

        int offset = renderedSquares * 4 * VERTEX_SIZE;

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

            vertices[offset + 2] = Float.intBitsToFloat(color);
            vertices[offset + 3] = Float.intBitsToFloat(biome.id() + (isDeleted ? (1 << 30) : 0));

            vertices[offset + 4] = UV_POSITIONS[i * 2] * ((relativeEndX - relativeStartX) + 1);
            vertices[offset + 5] = UV_POSITIONS[i * 2 + 1] * ((relativeEndZ - relativeStartZ) + 1);

            vertices[offset + 6] = textureLayer;

            offset += VERTEX_SIZE;
        }
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
