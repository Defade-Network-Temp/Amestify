package net.defade.amestify.graphics.gui;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.defade.amestify.graphics.Window;

public class ViewerGUI extends GUI {
    private final DatabaseConnectorGUI databaseConnectorGUI = new DatabaseConnectorGUI();
    private final WorldLoaderGUI worldLoaderGUI = new WorldLoaderGUI();
    private boolean isViewDisabled = false;

    @Override
    public void renderImgui(float deltaTime) {
        setupDockspace();

        if(!databaseConnectorGUI.isConnected()) {
            databaseConnectorGUI.renderImGui();
            disableView();
        } else {
            worldLoaderGUI.renderImGui();
        }

        enableView();

        ImGui.end(); // End the dockspace
    }

    @Override
    public boolean usesImGui() {
        return true;
    }

    private void setupDockspace() {
        int windowFlags = ImGuiWindowFlags.MenuBar | ImGuiWindowFlags.NoDocking;

        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(Window.getWidth(), Window.getHeight());
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0);
        windowFlags |= ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse |
                ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove |
                ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus;

        ImGui.begin("DockSpace", new ImBoolean(true), windowFlags);
        ImGui.popStyleVar(2);

        ImGui.dockSpace(ImGui.getID("DockSpace"));
    }

    private void enableView() {
        if(!isViewDisabled) return;
        isViewDisabled = false;
        ImGui.endDisabled();
    }

    private void disableView() {
        if(isViewDisabled) return;
        isViewDisabled = true;
        ImGui.beginDisabled(true);
    }
}
