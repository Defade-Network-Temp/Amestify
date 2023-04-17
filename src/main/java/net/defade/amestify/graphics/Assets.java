package net.defade.amestify.graphics;

import net.defade.amestify.graphics.texture.BiomeTexture;
import net.defade.amestify.graphics.texture.block.BlockSheet;
import net.defade.amestify.graphics.texture.block.generator.BlockTextureData;
import net.defade.amestify.graphics.texture.block.generator.BlockTextureMap;
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

    public static void init() throws IOException {
        Block.init();
        BiomeTexture.init();

        BlockTextureData[] blockTextureData = new BlockTextureMap().init();
        BLOCK_SHEET = new BlockSheet(blockTextureData, moveTexturesToTemp());

        CHUNK_SHADER = new Shader("map-shader.glsl");
        CHUNK_SHADER.init();

        GRID_SHADER = new Shader("grid-shader.glsl");
        GRID_SHADER.init();
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
