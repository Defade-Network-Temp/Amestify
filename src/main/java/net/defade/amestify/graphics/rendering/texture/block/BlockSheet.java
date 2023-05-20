package net.defade.amestify.graphics.rendering.texture.block;

import net.defade.amestify.graphics.rendering.texture.block.generator.BlockTextureData;
import net.defade.amestify.world.Block;
import org.lwjgl.BufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.stb.STBImage.*;

public class BlockSheet {
    private final int textureId;
    private final BlockTexture[] blocksStateIdsToTexture = new BlockTexture[Block.getStatesIdAmount()];

    public BlockSheet(BlockTextureData[] stateIdToTextureData, Path texturesDir) throws IOException {
        BlockTexture emptyTexture = new BlockTexture(-1, false, false, false, false, true);
        Arrays.fill(blocksStateIdsToTexture, emptyTexture);

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, textureId);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);

        int maxAnisotropy = glGetInteger(GL_MAX_TEXTURE_MAX_ANISOTROPY);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_ANISOTROPY, maxAnisotropy);

        List<Path> textures = getTexturesFromFolder(texturesDir);

        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA8, 16, 16,
                textures.size(), 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        stbi_set_flip_vertically_on_load(true);

        int textureLayer = 0;
        for (Path texture : textures) {
            BlockTextureData blockTextureData = null;
            List<Integer> stateIdsUsingTexture = new ArrayList<>();

            for (int stateId = 0; stateId < stateIdToTextureData.length; stateId++) {
                BlockTextureData stateIdBlockTextureData = stateIdToTextureData[stateId];
                if(stateIdBlockTextureData == null || stateIdBlockTextureData.textureFile() == null) continue;

                if(stateIdBlockTextureData.textureFile().equals(texture.getFileName().toString())) {
                    blockTextureData = stateIdBlockTextureData;
                    stateIdsUsingTexture.add(stateId);
                }
            }

            IntBuffer width = BufferUtils.createIntBuffer(1);
            IntBuffer height = BufferUtils.createIntBuffer(1);
            IntBuffer channels = BufferUtils.createIntBuffer(1);
            ByteBuffer image = stbi_load(texture.toString(), width, height, channels, 0);

            int imageFormat = channels.get(0) == 3 ? GL_RGB : GL_RGBA;

            if(image != null) {
                glTexSubImage3D(
                        GL_TEXTURE_2D_ARRAY, 0, 0, 0, textureLayer,
                        16, 16, 1, imageFormat,
                        GL_UNSIGNED_BYTE, image
                );
            } else {
                throw new RuntimeException("Could not load the image " + texture + ".");
            }

            // Using the imageFormat variable is not reliable as textures can have an alpha channel
            boolean isTranslucent = false;
            if(imageFormat == GL_RGBA) {
                for (int i = 0; i < 16 * 16; i++) {
                    if(image.get(i * 4 + 3) != (byte) 0xFF) {
                        isTranslucent = true;
                        break;
                    }
                }
            }

            stbi_image_free(image);

            for (Integer stateId : stateIdsUsingTexture) {
                blocksStateIdsToTexture[stateId] = new BlockTexture(
                        textureLayer,
                        blockTextureData.isGrass(),
                        blockTextureData.isFoliage(),
                        blockTextureData.isWater(),
                        isTranslucent,
                        blockTextureData.isIgnored()
                );
            }

            textureLayer++;
        }

        glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D_ARRAY, textureId);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
    }

    public BlockTexture getBlockTexture(int stateId) {
        return blocksStateIdsToTexture[stateId];
    }

    private static List<Path> getTexturesFromFolder(Path folder) throws IOException {
        Stream<Path> texturesStream = Files.list(folder).filter(path -> path.toString().endsWith(".png"));
        List<Path> textures = texturesStream.toList();
        texturesStream.close();

        return textures;
    }
}
