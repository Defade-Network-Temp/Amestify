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

    public static int floatToRGB(float[] rgb) {
        if(rgb.length != 3) throw new IllegalArgumentException("RGB array must have a length of 3");

        return floatToRGB(rgb[0], rgb[1], rgb[2]);
    }

    public static int floatToRGB(float r, float g, float b) {
        return (int) (r * 255) << 16 | (int) (g * 255) << 8 | (int) (b * 255);
    }

    public static float[] rgbToFloat(int rgb) {
        return rgbToFloat((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    public static float[] rgbToFloat(int red, int green, int blue) {
        return new float[] {red / 255f, green / 255f, blue / 255f};
    }
}
