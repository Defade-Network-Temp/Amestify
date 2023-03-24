package net.defade.amestify.graphics;

import net.defade.amestify.graphics.texture.block.BlockSheet;
import net.defade.amestify.graphics.texture.block.generator.BlockTextureData;
import net.defade.amestify.graphics.texture.block.generator.BlockTextureMap;
import net.defade.amestify.world.Block;
import net.defade.amestify.world.biome.Biome;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class Assets {
    public static BlockSheet BLOCK_SHEET;

    public static void init() throws IOException, URISyntaxException {
        Block.init();
        Biome.init();

        BlockTextureData[] blockTextureData = new BlockTextureMap().init();
        BLOCK_SHEET = new BlockSheet(blockTextureData, Path.of(Assets.class.getClassLoader().getResource("textures/blocks").toURI()));
    }
}
