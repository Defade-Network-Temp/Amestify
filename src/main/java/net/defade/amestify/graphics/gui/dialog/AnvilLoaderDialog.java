package net.defade.amestify.graphics.gui.dialog;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import net.defade.amestify.graphics.gui.Viewer;
import net.defade.amestify.utils.ProgressTracker;
import net.defade.amestify.utils.Utils;
import net.defade.amestify.world.RegionFile;
import net.defade.amestify.world.loaders.anvil.AnvilMapLoader;
import net.defade.amestify.world.viewer.MapViewerWorld;
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

public class AnvilLoaderDialog extends Dialog {
    private final Viewer viewer;

    private final ProgressTracker progressTracker = new ProgressTracker();

    private Path worldPath = null;
    private CompletableFuture<MapViewerWorld> worldFuture;

    public AnvilLoaderDialog(Viewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public void render() {
        if(worldFuture == null || worldFuture.isCompletedExceptionally()) {
            renderSelectWorldDialog();
        } else {
            if(!worldFuture.isDone()) {
                renderWorldLoadingDialog();
            }
        }
    }

    private void renderSelectWorldDialog() {
        ImGui.setNextWindowSize(200, 115);
        ImGui.begin("Select Anvil world", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoResize);

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

        Utils.imGuiAlignNextItem(buttonText, 0);
        if(isWorldButtonRed) ImGui.pushStyleColor(ImGuiCol.Text, 255, 0, 0, 255);
        if(ImGui.button(buttonText)) {
            openFolderDialog();
        }
        if(isWorldButtonRed) ImGui.popStyleColor();

        if(isPathValid()) {
            Utils.imGuiAlignNextItem("Load", 0);
            if(ImGui.button("Load")) {
                worldFuture = RegionFile.convert(progressTracker, new AnvilMapLoader(worldPath, -64, 320)).whenComplete((mapViewerWorld, throwable) -> {
                    if(throwable != null) {
                        throwable.printStackTrace();
                    } else {
                        viewer.setMapViewerWorld(mapViewerWorld);
                        disable();
                    }
                });
            }
        }

        if(worldFuture != null && worldFuture.isCompletedExceptionally()) {
            ImGui.pushStyleColor(ImGuiCol.Text, 255, 0, 0, 255);
            ImGui.textWrapped(worldFuture.handle((mapViewerWorld, throwable) -> throwable.getMessage()).join());
            ImGui.popStyleColor();
        }

        Utils.imGuiAlignNextItem("Cancel", 1);
        ImGui.pushStyleColor(ImGuiCol.Button, 215, 45, 45, 255);
        if(ImGui.button("Cancel")) disable();
        ImGui.popStyleColor();

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
        ImGui.begin("Loading anvil world...", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoResize);

        Utils.imGuiProgressBar(progressTracker);

        ImGui.end();
    }

    public MapViewerWorld getWorld() {
        return worldFuture.join();
    }

    @Override
    protected void reset() {
        viewer.setMapViewerWorld(null);
        worldFuture = null;
    }
}
