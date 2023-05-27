package net.defade.amestify.graphics.rendering.texture;

import org.lwjgl.BufferUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture {
    private final int textureId;

    public Texture(Path path) throws IOException {
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);

        int maxAnisotropy = glGetInteger(GL_MAX_TEXTURE_MAX_ANISOTROPY);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY, maxAnisotropy);

        stbi_set_flip_vertically_on_load(false);
        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        InputStream textureInputStream = Texture.class.getClassLoader().getResourceAsStream("textures/" + path);
        if(textureInputStream == null) {
            throw new RuntimeException("Texture not found");
        }

        byte[] textureBytes = textureInputStream.readAllBytes();
        ByteBuffer textureBuffer = BufferUtils.createByteBuffer(textureBytes.length);
        textureBuffer.put(textureBytes);
        textureBuffer.flip();
        textureInputStream.close();

        ByteBuffer texture = stbi_load_from_memory(textureBuffer, width, height, channels, 0);
        if(texture != null) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(), height.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, texture);
            glGenerateMipmap(GL_TEXTURE_2D);
            stbi_image_free(texture);
        }
    }

    public int getTextureId() {
        return textureId;
    }
}
