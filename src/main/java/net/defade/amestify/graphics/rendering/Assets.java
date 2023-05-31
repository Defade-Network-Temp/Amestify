package net.defade.amestify.graphics.rendering;

import net.defade.amestify.graphics.rendering.texture.BiomeTexture;
import net.defade.amestify.graphics.rendering.texture.Texture;
import net.defade.amestify.graphics.rendering.texture.block.BlockSheet;
import net.defade.amestify.graphics.rendering.texture.block.generator.BlockTextureData;
import net.defade.amestify.graphics.rendering.texture.block.generator.BlockTextureMap;
import net.defade.amestify.world.Block;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Assets {
    public static BlockSheet BLOCK_SHEET;

    public static Shader CHUNK_SHADER;
    public static Shader GRID_SHADER;
    public static Shader SHAPE_SHADER;

    public static Texture EDIT_ICON;
    public static Texture DONE_ICON;
    public static Texture OPEN_ICON;
    public static Texture GRID_SELECTION_ICON;
    public static Texture BLOCK_BIOME_EDITOR_ICON;
    public static Texture DELETE_CHUNK_ICON;

    public static void init() throws IOException {
        Block.init();
        BiomeTexture.init();

        BlockTextureData[] blockTextureData = new BlockTextureMap().init();
        BLOCK_SHEET = new BlockSheet(blockTextureData, moveTexturesToTemp());

        CHUNK_SHADER = new Shader("map-shader.glsl");
        CHUNK_SHADER.init();

        GRID_SHADER = new Shader("grid-shader.glsl");
        GRID_SHADER.init();

        SHAPE_SHADER = new Shader("shape-shader.glsl");
        SHAPE_SHADER.init();

        EDIT_ICON = new Texture(Paths.get("gui", "edit.png"));
        DONE_ICON = new Texture(Paths.get("gui", "done.png"));
        OPEN_ICON = new Texture(Paths.get("gui", "open.png"));
        GRID_SELECTION_ICON = new Texture(Paths.get("gui", "grid-selection.png"));
        BLOCK_BIOME_EDITOR_ICON = new Texture(Paths.get("gui", "block-biome-editor.png"));
        DELETE_CHUNK_ICON = new Texture(Paths.get("gui", "delete-chunk.png"));
    }

    // Java can't list files inside the JAR, so we copy them to a temp folder.
    private static Path moveTexturesToTemp() throws IOException {
        InputStream blocksTextureInputStream = Assets.class.getClassLoader().getResourceAsStream("textures/blocks.zip");
        if(blocksTextureInputStream == null) throw new FileNotFoundException("Couldn't find textures/blocks.zip");
        byte[] blocksTextureZip = blocksTextureInputStream.readAllBytes();
        blocksTextureInputStream.close();

        Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"), "amestify", "textures");
        if(!Files.exists(tempPath)) Files.createDirectories(tempPath);

        Path blocksZipPath = tempPath.resolve("blocks.zip");
        Files.write(blocksZipPath, blocksTextureZip);

        unzip(blocksZipPath, tempPath.resolve("blocks"));

        return tempPath.resolve("blocks");
    }

    private static void unzip(Path zipFilePath, Path destinationDirectory) throws IOException {
        if(!Files.exists(destinationDirectory) || !Files.isDirectory(destinationDirectory)) Files.createDirectory(destinationDirectory);
        InputStream zipFileInputStream = Files.newInputStream(zipFilePath);
        ZipInputStream zipInputStream = new ZipInputStream(zipFileInputStream);

        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while(zipEntry != null){
            String fileName = zipEntry.getName();
            Path newFile = destinationDirectory.resolve(fileName);

            //create directories for sub directories in zip
            if(!Files.exists(newFile.getParent()) || !Files.isDirectory(newFile.getParent())) Files.createDirectory(newFile.getParent());
            Files.write(newFile, zipInputStream.readAllBytes());

            zipInputStream.closeEntry();
            zipEntry = zipInputStream.getNextEntry();
        }

        zipInputStream.closeEntry();
        zipInputStream.close();
        zipFileInputStream.close();
    }
}
