package net.defade.amestify.graphics.gui.renderer;

import net.defade.amestify.graphics.rendering.Assets;
import net.defade.amestify.graphics.rendering.Camera;

import static org.lwjgl.opengl.GL46.*;

public class ShapeRenderer {
    private static final int POS_SIZE = 2;
    private static final int COLOR_SIZE = 1;

    private static final int POS_OFFSET = 0;
    private static final int COLOR_OFFSET = POS_OFFSET + POS_SIZE * Float.BYTES;

    private static final int VERTEX_SIZE = POS_SIZE + COLOR_SIZE;
    private static final int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES;

    private int vaoID, vboID;

    private int triangles = 0;

    private boolean buffersInitialized = false;

    private float[] vertices = new float[0];

    private void initBuffers() {
        vboID = glCreateBuffers();
        vaoID = glCreateVertexArrays();

        glVertexArrayVertexBuffer(vaoID, 0, vboID, 0, VERTEX_SIZE_BYTES);

        glEnableVertexArrayAttrib(vaoID, 0);
        glEnableVertexArrayAttrib(vaoID, 1);

        glVertexArrayAttribFormat(vaoID, 0, POS_SIZE, GL_FLOAT, false, POS_OFFSET);
        glVertexArrayAttribFormat(vaoID, 1, COLOR_SIZE, GL_FLOAT, false, COLOR_OFFSET);

        glVertexArrayAttribBinding(vaoID, 0, 0);
        glVertexArrayAttribBinding(vaoID, 1, 0);
        buffersInitialized = true;
    }

    public void addSquare(int startX, int startZ, int endX, int endZ, int argbColor) {
        float color = Float.intBitsToFloat(argbColor);

        startX *= 16;
        startZ *= 16;
        endX *= 16;
        endZ *= 16;

        int offset = triangles * 3 * VERTEX_SIZE;

        growBuffer(2);

        vertices[offset] = startX;
        vertices[offset + 1] = startZ;
        vertices[offset + 2] = color;

        vertices[offset + 3] = endX;
        vertices[offset + 4] = startZ;
        vertices[offset + 5] = color;

        vertices[offset + 6] = endX;
        vertices[offset + 7] = endZ;
        vertices[offset + 8] = color;


        vertices[offset + 9] = startX;
        vertices[offset + 10] = startZ;
        vertices[offset + 11] = color;

        vertices[offset + 12] = startX;
        vertices[offset + 13] = endZ;
        vertices[offset + 14] = color;

        vertices[offset + 15] = endX;
        vertices[offset + 16] = endZ;
        vertices[offset + 17] = color;
    }

    private void growBuffer(int triangles) {
        float[] vertices = new float[this.vertices.length + (3 * triangles * VERTEX_SIZE)];
        System.arraycopy(this.vertices, 0, vertices, 0, this.vertices.length);
        this.vertices = vertices;

        this.triangles += triangles;
    }

    public void render(Camera camera) {
        Assets.SHAPE_SHADER.attach();
        Assets.SHAPE_SHADER.uploadMat4f("projectionUniform", camera.getProjectionMatrix());
        Assets.SHAPE_SHADER.uploadMat4f("viewUniform", camera.getViewMatrix());

        if(!buffersInitialized) initBuffers();

        glNamedBufferData(vboID, vertices, GL_DYNAMIC_DRAW);

        glBindVertexArray(vaoID);
        glDrawArrays(GL_TRIANGLES, 0,triangles * 3);

        triangles = 0;
        vertices = new float[0];

        Assets.SHAPE_SHADER.detach();
    }
}
