package net.defade.amestify.graphics.gui;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import net.defade.amestify.loaders.anvil.AnvilMap;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NativeFileDialog;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class WorldLoaderGUI {
    private Path worldPath = null;
    private AnvilMap anvilMap = null;

    public void renderImGui() {
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
                System.out.println("load");
            }
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

    private void centerNextItem(String label) {
        ImGuiStyle style = ImGui.getStyle();

        float size = ImGui.calcTextSize(label).x + style.getFramePadding().x * 2.0f;
        float avail = ImGui.getContentRegionAvail().x;

        float off = (avail - size) * 0.5f;
        if (off > 0.0f) ImGui.setCursorPosX(ImGui.getCursorPosX() + off);
    }
}
