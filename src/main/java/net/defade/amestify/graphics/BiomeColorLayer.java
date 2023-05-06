package net.defade.amestify.graphics;

import net.defade.amestify.world.biome.Biome;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.opengl.GL46.*;

public class BiomeColorLayer {
    private int bufferId = -1;
    private int[] colors = new int[0];
    private boolean isDirty = true;

    public void registerBiome(Biome biome) {
        while (true) {
            int color =
                    ThreadLocalRandom.current().nextInt(0, 255) << 16 |
                    ThreadLocalRandom.current().nextInt(0, 255) << 8 |
                    ThreadLocalRandom.current().nextInt(0, 255);

            if(Arrays.stream(colors).noneMatch(biomeColor -> biomeColor == color)) {
                if(colors.length <= biome.id()) {
                    int[] newColors = new int[biome.id() + 1];
                    System.arraycopy(colors, 0, newColors, 0, colors.length);
                    colors = newColors;
                }

                setBiomeColor(biome, color);
                break;
            }
        }
    }

    public void setBiomeColor(Biome biome, int color) {
        colors[biome.id()] = color;
        isDirty = true;
    }

    public int getColor(Biome biome) {
        return colors[biome.id()];
    }

    public void bind() {
        if(bufferId == -1) bufferId = glCreateBuffers();
        if(isDirty) {
            FloatBuffer buffer = BufferUtils.createFloatBuffer(colors.length * 4);
            for (int color : colors) {
                buffer.put((color >> 16 & 0xFF) / 255f);
                buffer.put((color >> 8 & 0xFF) / 255f);
                buffer.put((color & 0xFF) / 255f);
                buffer.put(0); // OpenGL padding
                // We use vec4s because OpenGL behaves weirdly with vec3s as they're internally stored as vec4s
            }
            buffer.flip();
            glNamedBufferData(bufferId, buffer, GL_STATIC_DRAW);
            isDirty = false;
        }

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, bufferId);
    }

    public void unbind() {
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
    }
}
