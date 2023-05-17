package net.defade.amestify.graphics.gui.viewer;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.world.MapViewerWorld;
import net.defade.amestify.world.savers.AmethystSaver;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NativeFileDialog;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class AmethystSaveFileGUI {
    private final AmethystSaver amethystSaver = new AmethystSaver();
    private MapViewerWorld world;

    private Path saveFilePath;
    private final ImString config = new ImString();
    private boolean isModifyingConfig = false;

    private final ProgressTracker progressTracker = new ProgressTracker();
    private CompletableFuture<Path> saveFuture;

    private boolean hasUserAcknowledgedSave = true;

    public void renderImGui() {
        if(saveFuture == null || saveFuture.isCompletedExceptionally()) {
            renderSaveFileDialog();
        } else if(!saveFuture.isDone()) {
            renderSavingDialog();
        } else {
            if(!hasUserAcknowledgedSave) {
                renderSaveCompleteDialog();
            }
        }
    }

    public boolean isDone() {
        return saveFuture == null && hasUserAcknowledgedSave;
    }

    public void setWorld(MapViewerWorld world) {
        this.world = world;
    }

    public void reset() {
        hasUserAcknowledgedSave = false;
    }

    private void renderSaveFileDialog() {
        ImGui.begin("Save world", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking);

        if(saveFuture != null && saveFuture.isCompletedExceptionally()) {
            String errorMessage = saveFuture.handle((path, throwable) -> throwable.getMessage()).join();
            centerNextItem(errorMessage);
            ImGui.textColored(255, 0, 0, 255, errorMessage);
        }

        if(saveFilePath != null && Files.exists(saveFilePath)) {
            String warningText = "Warning: File already exists! It will be overwritten.";
            centerNextItem(warningText);
            ImGui.textColored(255, 200, 0, 255, warningText);
        }

        String buttonText = saveFilePath == null ? "Select destination file" : saveFilePath.getFileName().toString();
        centerNextItem(buttonText);
        if(ImGui.button(buttonText)) {
            openSaveFileDialog();
        }

        if(ImGui.button("Modify config")) isModifyingConfig = true;

        centerNextItem("Save");
        if(saveFilePath == null) ImGui.beginDisabled();
        if(ImGui.button("Save")) {
            String config = new String(this.config.getData()); // ImString generates the String
            // when the dirty flag is set to true, but the text dialog doesn't update this flag,
            // so we can't just get the String from the ImString#get() method...

            saveFuture = amethystSaver.saveToTempFile(saveFilePath.getFileName().toString(), config, world, progressTracker);
            saveFuture.thenApply(path -> {
                if(path != null) {
                    try {
                        if(Files.exists(saveFilePath)) Files.delete(saveFilePath);
                        Files.move(path, saveFilePath);
                    } catch (IOException exception) {
                        return exception;
                    }
                }

                return path;
            });

            hasUserAcknowledgedSave = false;
        }
        if(saveFilePath == null) ImGui.endDisabled();

        ImGui.end();

        if(isModifyingConfig) {
            ImGui.begin("Config", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking);
            if (ImGui.button("Close", 80, 0)) isModifyingConfig = false;
            ImGui.inputTextMultiline("##Config", config, -1, -1, ImGuiInputTextFlags.AllowTabInput | ImGuiInputTextFlags.AutoSelectAll | ImGuiInputTextFlags.EnterReturnsTrue);
            ImGui.end();
        }
    }

    private void renderSavingDialog() {
        ImGui.setNextWindowSize(600, 100);
        ImGui.begin("Saving world...", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoResize);

        String text = "Saved " + progressTracker.getCurrent() + " out of " + progressTracker.getTotal() + " chunks (" +
                (int) (progressTracker.getProgress() * 100) + "%)";
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

        ImGui.end();
    }

    private void renderSaveCompleteDialog() {
        ImGui.setNextWindowSize(80, 60);
        ImGui.begin("Complete!", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoResize);

        centerNextItem("Close");
        if(ImGui.button("Close")) {
            hasUserAcknowledgedSave = true;
            saveFuture = null;
        }

        ImGui.end();
    }

    private void openSaveFileDialog() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pPath = stack.callocPointer(1);
            int fileDialogResult = NativeFileDialog.NFD_SaveDialog((ByteBuffer) null, null, pPath);
            if (fileDialogResult == NativeFileDialog.NFD_OKAY) {
                String path = MemoryUtil.memUTF8(pPath.get(0));
                saveFilePath = Path.of(path);
            }

            NativeFileDialog.nNFD_Free(pPath.get(0));
        }
    }

    private static void centerNextItem(String label) {
        ImGuiStyle style = ImGui.getStyle();

        float size = ImGui.calcTextSize(label).x + style.getFramePadding().x * 2.0f;
        float avail = ImGui.getContentRegionAvail().x;

        float off = (avail - size) * 0.5f;
        if (off > 0.0f) ImGui.setCursorPosX(ImGui.getCursorPosX() + off);
    }
}
