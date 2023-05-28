package net.defade.amestify.utils;

import imgui.ImGui;
import imgui.ImGuiStyle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils {
    public static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir") + File.separator + "amestify");

    public static int bitsToRepresent(int n) {
        return Integer.SIZE - Integer.numberOfLeadingZeros(n);
    }

    public static double clamp(double min, double max, double value) {
        return Math.max(min, Math.min(max, value));
    }

    public static float lerp(float start, float end, float t) {
        return start + t * (end - start);
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

    /**
     * Set the next item's alignment
     * @param label The label of the item
     * @param alignment 0 = center, 1 = right
     */
    public static void imGuiAlignNextItem(String label, int alignment) {
        ImGuiStyle style = ImGui.getStyle();

        float size = ImGui.calcTextSize(label).x + style.getFramePadding().x * 2.0f;
        float avail = ImGui.getContentRegionAvail().x;

        if (alignment == 0) {
            float off = (avail - size) * 0.5f;
            if (off > 0.0f) ImGui.setCursorPosX(ImGui.getCursorPosX() + off);
        } else if (alignment == 1) {
            float off = avail - size;
            if (off > 0.0f) ImGui.setCursorPosX(ImGui.getCursorPosX() + off);
        }
    }

    public static void imGuiProgressBar(ProgressTracker progressTracker) {
        String text = progressTracker.getMessage() == null ? "..." : progressTracker.getMessage();
        float windowWidth = ImGui.getWindowSizeX();
        float windowHeight = ImGui.getWindowSizeY();
        float width = 600;
        float height = 30;
        ImGui.setCursorPosX((windowWidth - width) / 2);
        ImGui.setCursorPosY((windowHeight - height) / 2);

        ImGui.progressBar(progressTracker.getProgress(), width, height, "");
        ImGui.sameLine(
                (windowWidth - width) / 2 // Set text start at the start of the progress bar
                        + (width / 2)  // Set text start at the middle of the progress bar
                        - (ImGui.calcTextSize(text).x / 2) // Set text at the middle of the text
        );
        ImGui.text(text);
    }

    public static long getChunkIndex(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xffffffffL);
    }

    public static int getChunkCoordX(long index) {
        return (int) (index >> 32);
    }

    public static int getChunkCoordZ(long index) {
        return (int) index;
    }

    public static String convertFileSizeUnit(long bytes) {
        if(bytes < 1024 * 1024) {
            return Math.round(bytes / 1024D * 100) / 100D + "ko";
        } else if(bytes < 1024 * 1024 * 1024) {
            return Math.round(bytes / 1024D / 1024D * 100) / 100D + "mo";
        } else {
            return Math.round(bytes / 1024D / 1024D / 1024D * 100) / 100D + "go";
        }
    }
}
