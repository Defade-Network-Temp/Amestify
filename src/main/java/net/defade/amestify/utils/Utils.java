package net.defade.amestify.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils {
    public static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir") + File.separator + "amestify");

    public static int bitsToRepresent(int n) {
        return Integer.SIZE - Integer.numberOfLeadingZeros(n);
    }

    public static Path createAmethystTempFile(String fileName) throws IOException {
        if(!Files.exists(TEMP_DIR) || !Files.isDirectory(TEMP_DIR)) {
            try {
                Files.deleteIfExists(TEMP_DIR);
                Files.createDirectory(TEMP_DIR);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        Path filePath = Path.of(TEMP_DIR + File.separator + fileName);

        Files.deleteIfExists(filePath);
        return Files.createFile(filePath);
    }
}
