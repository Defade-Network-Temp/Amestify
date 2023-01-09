package net.defade.amestify.map;

import net.defade.yokura.amethyst.AmethystSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class AmethystFileSource implements AmethystSource {
    private static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir") + File.separator + "amestify");

    private final Path file;

    public AmethystFileSource(String fileName) {
        if(!Files.exists(TEMP_DIR) || !Files.isDirectory(TEMP_DIR)) {
            try {
                Files.deleteIfExists(TEMP_DIR);
                Files.createDirectory(TEMP_DIR);
            } catch (IOException exception) {
                exception.printStackTrace(); // TODO
            }
        }
        this.file = TEMP_DIR.resolve(fileName);
    }

    @Override
    public InputStream getSource() {
        return null;
    }

    @Override
    public OutputStream getOutputStream(CompletableFuture<Void> writeFuture) {
        try {
            if(Files.exists(file) || Files.isDirectory(file)) {
                Files.delete(file);
            }
            Files.createFile(file);

            return Files.newOutputStream(file);
        } catch (Throwable exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public Path getFile() {
        return file;
    }
}
