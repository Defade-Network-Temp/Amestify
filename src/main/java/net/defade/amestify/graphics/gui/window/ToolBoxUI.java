package net.defade.amestify.graphics.gui.window;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import net.defade.amestify.graphics.gui.Viewer;
import net.defade.amestify.graphics.gui.tools.Tool;

public class ToolBoxUI extends UIComponent {
    private static final int ICON_SIZE = 32;

    private final Viewer viewer;

    public ToolBoxUI(Viewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public void render() {
        ImGui.begin("Tool Box");

        int windowSizeX = (int) (ImGui.getWindowSizeX() - ImGui.getStyle().getWindowPaddingX());
        for (Tool tool : viewer.getTools()) {
            ImGui.pushID(tool.getName());
            if (tool.isActive()) {
                ImGui.imageButton(
                        tool.getIcon().getTextureId(), ICON_SIZE, ICON_SIZE,
                        0, 0, 1, 1,
                        0,
                        1, 1, 1, 1,
                        0, 0, 0, 1
                );
            } else {
                ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImGui.getColorU32(0.5f, 0.5f, 0.5f, 0.5f));
                ImGui.pushStyleColor(ImGuiCol.Button, ImGui.getColorU32(0, 0, 0, 1));

                if (ImGui.imageButton(
                        tool.getIcon().getTextureId(), ICON_SIZE, ICON_SIZE,
                        0, 0, 1, 1,
                        0
                )) {
                    viewer.useTool(tool);
                }

                ImGui.popStyleColor(2);
            }
            ImGui.popID();
            if(ImGui.isItemHovered()) {
                ImVec2 mousePos = ImGui.getMousePos();

                ImGui.begin("Tool Name", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoMove);
                ImGui.setWindowPos(mousePos.x + 5, mousePos.y + 5); // Adjust position to offset from the mouse cursor,
                // else it'll sit under the mouse, causing flickering because the gui will be the hovered item
                ImGui.text(tool.getName());
                ImGui.end();
            }

            windowSizeX -= ICON_SIZE + ImGui.getStyle().getWindowPaddingX();
            if (windowSizeX >= ICON_SIZE + ImGui.getStyle().getWindowPaddingX() + ImGui.getStyle().getScrollbarSize()) {
                ImGui.sameLine();
            } else {
                windowSizeX = (int) (ImGui.getWindowSizeX() - ImGui.getStyle().getWindowPaddingX());
            }
        }

        ImGui.end();
    }
}
