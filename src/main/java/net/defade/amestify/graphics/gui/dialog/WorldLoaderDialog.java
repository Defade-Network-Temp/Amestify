package net.defade.amestify.graphics.gui.dialog;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import net.defade.amestify.graphics.gui.Viewer;
import net.defade.amestify.utils.Utils;
import net.defade.amestify.world.AnvilToViewerRegionConverter;
import net.defade.amestify.world.MapViewerWorld;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NativeFileDialog;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class WorldLoaderDialog extends Dialog {
    private final Viewer viewer;

    private Path worldPath = null;
    private final AnvilToViewerRegionConverter anvilToViewerRegionConverter = new AnvilToViewerRegionConverter();

    public WorldLoaderDialog(Viewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public void render() {
        if(anvilToViewerRegionConverter.getMapViewerWorldFuture() == null || anvilToViewerRegionConverter.getMapViewerWorldFuture().isCompletedExceptionally()) {
            renderSelectWorldDialog();
        } else {
            if(!anvilToViewerRegionConverter.getMapViewerWorldFuture().isDone()) {
                renderWorldLoadingDialog();
            }
        }

        if(anvilToViewerRegionConverter.getMapViewerWorldFuture() != null && anvilToViewerRegionConverter.getMapViewerWorldFuture().isDone()) {
            viewer.setMapViewerWorld(anvilToViewerRegionConverter.getMapViewerWorldFuture().join());
            disable();
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

        Utils.imGuiCenterNextItem(buttonText);
        if(isWorldButtonRed) ImGui.pushStyleColor(ImGuiCol.Text, 255, 0, 0, 255);
        if(ImGui.button(buttonText)) {
            openFolderDialog();
        }
        if(isWorldButtonRed) ImGui.popStyleColor();

        if(isPathValid()) {
            Utils.imGuiCenterNextItem("Load");
            if(ImGui.button("Load")) {
                anvilToViewerRegionConverter.convert(worldPath);
            }
        }

        if(anvilToViewerRegionConverter.getMapViewerWorldFuture() != null && anvilToViewerRegionConverter.getMapViewerWorldFuture().isCompletedExceptionally()) {
            ImGui.pushStyleColor(ImGuiCol.Text, 255, 0, 0, 255);
            ImGui.textWrapped(anvilToViewerRegionConverter.getMapViewerWorldFuture().handle((mapViewerWorld, throwable) -> throwable.getMessage()).join());
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

        String text = "Loaded " + anvilToViewerRegionConverter.getProgressTracker().getCurrent() + " out of " +
                anvilToViewerRegionConverter.getProgressTracker().getTotal() + " chunks (" +
                (int) (anvilToViewerRegionConverter.getProgressTracker().getProgress() * 100) + "%)";
        Utils.imGuiProgressBar(text, anvilToViewerRegionConverter.getProgressTracker());

        ImGui.end();
    }

    public MapViewerWorld getWorld() {
        return anvilToViewerRegionConverter.getMapViewerWorldFuture().join();
    }

    @Override
    protected void reset() {
        anvilToViewerRegionConverter.reset();
        viewer.setMapViewerWorld(null);
    }
}
