package net.defade.amestify.graphics;

import static org.lwjgl.opengl.GL46.*;

public class Framebuffer {
    private final int width;
    private final int height;
    private final int textureId;

    private final int fboID;

    public Framebuffer(int width, int height) {
        this.width = width;
        this.height = height;
        this.textureId = initTexture();

        fboID = glCreateFramebuffers();
        glNamedFramebufferTexture(fboID, GL_COLOR_ATTACHMENT0, textureId, 0);

        int rboId = glCreateRenderbuffers();
        glNamedRenderbufferStorage(rboId, GL_DEPTH_COMPONENT32, width, height);
        glNamedFramebufferRenderbuffer(fboID, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboId);

        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Could not create framebuffer.");
        }
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFboID() {
        return fboID;
    }

    public int getTextureId() {
        return textureId;
    }

    private int initTexture() {
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);

        return textureId;
    }
}
