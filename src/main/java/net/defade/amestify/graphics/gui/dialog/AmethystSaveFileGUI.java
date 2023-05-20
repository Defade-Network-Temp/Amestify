package net.defade.amestify.graphics.gui.dialog;

import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import net.defade.amestify.graphics.gui.Viewer;
import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.utils.Utils;
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

public class AmethystSaveFileGUI extends Dialog {
    private final Viewer viewer;
    private final AmethystSaver amethystSaver = new AmethystSaver();

    private Path saveFilePath;
    private final ImString config = new ImString();
    private boolean isModifyingConfig = false;

    private final ProgressTracker progressTracker = new ProgressTracker();
    private CompletableFuture<Path> saveFuture;

    public AmethystSaveFileGUI(Viewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public void render() {
        if(saveFuture == null || saveFuture.isCompletedExceptionally()) {
            renderSaveFileDialog();
        } else if(!saveFuture.isDone()) {
            renderSavingDialog();
        } else {
            renderSaveCompleteDialog();
        }
    }

    private void renderSaveFileDialog() {
        ImGui.begin("Save world", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking);

        if(saveFuture != null && saveFuture.isCompletedExceptionally()) {
            String errorMessage = saveFuture.handle((path, throwable) -> throwable.getMessage()).join();
            Utils.imGuiCenterNextItem(errorMessage);
            ImGui.textColored(255, 0, 0, 255, errorMessage);
        }

        if(saveFilePath != null && Files.exists(saveFilePath)) {
            String warningText = "Warning: File already exists! It will be overwritten.";
            Utils.imGuiCenterNextItem(warningText);
            ImGui.textColored(255, 200, 0, 255, warningText);
        }

        String buttonText = saveFilePath == null ? "Select destination file" : saveFilePath.getFileName().toString();
        Utils.imGuiCenterNextItem(buttonText);
        if(ImGui.button(buttonText)) {
            openSaveFileDialog();
        }

        if(ImGui.button("Modify config")) isModifyingConfig = true;

        Utils.imGuiCenterNextItem("Save");
        if(saveFilePath == null) ImGui.beginDisabled();
        if(ImGui.button("Save")) {
            String config = new String(this.config.getData()); // ImString generates the String
            // when the dirty flag is set to true, but the text dialog doesn't update this flag,
            // so we can't just get the String from the ImString#get() method...

            saveFuture = amethystSaver.saveToTempFile(saveFilePath.getFileName().toString(), config, viewer.getMapViewerWorld(), progressTracker);
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
        Utils.imGuiProgressBar(text, progressTracker);

        ImGui.end();
    }

    private void renderSaveCompleteDialog() {
        ImGui.setNextWindowSize(80, 60);
        ImGui.begin("Complete!", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoResize);

        Utils.imGuiCenterNextItem("Close");
        if(ImGui.button("Close")) {
            saveFuture = null;
            disable();
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
}
