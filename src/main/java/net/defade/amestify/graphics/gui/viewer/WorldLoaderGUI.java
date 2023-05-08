package net.defade.amestify.graphics.gui.viewer;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import net.defade.amestify.loaders.anvil.AnvilMapLoader;
import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.world.World;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NativeFileDialog;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class WorldLoaderGUI {
    private Path worldPath = null;
    private final ProgressTracker progressTracker = new ProgressTracker();

    private CompletableFuture<World> worldFuture = null;
    private Throwable worldFutureException = null;

    public void renderImGui() {
        if(worldFuture == null) {
            renderSelectWorldDialog();
        } else {
            if(!worldFuture.isDone()) {
                renderWorldLoadingDialog();
            }
        }
    }

    private void renderSelectWorldDialog() {
        ImGui.setNextWindowSize(200, 100);
        ImGui.begin("Select world", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoResize);

        String buttonText;
        boolean isWorldButtonRed = false;
        if(worldPath == null) {
            buttonText = "Select world";
        } else {
            if(!isPathValid()) {
                buttonText = worldPath.getFileName().toString() + " (invalid world)";
                isWorldButtonRed = true;
            } else {
                buttonText = worldPath.getFileName().toString();
            }
        }

        centerNextItem(buttonText);
        if(isWorldButtonRed) ImGui.pushStyleColor(ImGuiCol.Text, 255, 0, 0, 255);
        if(ImGui.button(buttonText)) {
            openFolderDialog();
        }
        if(isWorldButtonRed) ImGui.popStyleColor();

        if(isPathValid()) {
            centerNextItem("Load");
            if(ImGui.button("Load")) {
                worldFuture = new AnvilMapLoader(worldPath, -64, 320).loadWorld(progressTracker);
                worldFuture.whenComplete((world, throwable) -> {
                    if(throwable != null) {
                        worldFutureException = throwable;
                        worldFuture = null;
                    }
                });
            }
        }

        if(worldFutureException != null) {
            ImGui.pushStyleColor(ImGuiCol.Text, 255, 0, 0, 255);
            ImGui.textWrapped(worldFutureException.getMessage());
            ImGui.popStyleColor();
        }

        ImGui.end();
    }

    private void openFolderDialog() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pPath = stack.callocPointer(1);
            int fileDialogResult = NativeFileDialog.NFD_PickFolder((ByteBuffer) null, pPath);
            if (fileDialogResult == NativeFileDialog.NFD_OKAY) {
                String path = MemoryUtil.memUTF8(pPath.get(0));
                worldPath = Path.of(path);
            }

            NativeFileDialog.nNFD_Free(pPath.get(0));
        }
    }

    private boolean isPathValid() {
        if(worldPath == null) return false;
        if(!Files.exists(worldPath) || !Files.isDirectory(worldPath)) return false;

        Path regionFolder = worldPath.resolve("region");
        if(!Files.exists(regionFolder) || !Files.isDirectory(regionFolder)) return false;

        try(Stream<Path> files = Files.list(regionFolder)) {
            if(!files.allMatch(path -> path.getFileName().toString().endsWith(".mca"))) return false;
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }

        return true;
    }

    private void renderWorldLoadingDialog() {
        ImGui.setNextWindowSize(600, 100);
        ImGui.begin("Loading world...", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoResize);

        String text = "Loaded " + progressTracker.getCurrent() + " out of " +
                progressTracker.getTotal() + " chunks (" +
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

    private void centerNextItem(String label) {
        ImGuiStyle style = ImGui.getStyle();

        float size = ImGui.calcTextSize(label).x + style.getFramePadding().x * 2.0f;
        float avail = ImGui.getContentRegionAvail().x;

        float off = (avail - size) * 0.5f;
        if (off > 0.0f) ImGui.setCursorPosX(ImGui.getCursorPosX() + off);
    }

    public boolean isDone() {
        return worldFuture != null && worldFuture.isDone() && !worldFuture.isCompletedExceptionally();
    }

    public World getWorld() {
        return worldFuture.join();
    }

    public void reset() {
        worldFuture = null;
        worldFutureException = null;
    }
}
