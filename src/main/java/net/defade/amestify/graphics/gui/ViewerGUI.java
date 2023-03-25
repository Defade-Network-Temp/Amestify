package net.defade.amestify.graphics.gui;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.defade.amestify.graphics.Assets;
import net.defade.amestify.graphics.Camera;
import net.defade.amestify.graphics.Framebuffer;
import net.defade.amestify.graphics.Window;
import net.defade.amestify.graphics.gui.map.RegionRenderer;
import net.defade.amestify.world.World;
import net.defade.amestify.world.chunk.pos.RegionPos;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL46.*;

public class ViewerGUI extends GUI {
    private final DatabaseConnectorGUI databaseConnectorGUI = new DatabaseConnectorGUI();
    private final WorldLoaderGUI worldLoaderGUI = new WorldLoaderGUI();
    private boolean isViewDisabled = false;

    private final Map<RegionPos, RegionRenderer> regionRenderers = new HashMap<>();
    private final Framebuffer framebuffer = new Framebuffer(1920, 1080);

    private final Camera camera = new Camera();
    private World world;


    @Override
    public void renderImgui(float deltaTime) {
        setupDockspace();

        if(!databaseConnectorGUI.isConnected()) {
            databaseConnectorGUI.renderImGui();
            disableView();
        } else if(!worldLoaderGUI.isDone()){
            worldLoaderGUI.renderImGui();
            disableView();
        } else {
            if(world == null) {
                world = worldLoaderGUI.getWorld();
                world.getRegions().forEach((region) -> {
                    RegionRenderer regionRenderer = new RegionRenderer(region);
                    regionRenderer.init();
                    regionRenderers.put(region.getRegionPos(), regionRenderer);
                });
            }
        }

        renderMap();

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

    private void renderMap() {
        framebuffer.bind();

        glClearColor(1, 1, 1, 1);
        glClear(GL_COLOR_BUFFER_BIT);

        Assets.CHUNK_SHADER.attach();
        Assets.CHUNK_SHADER.uploadMat4f("projectionUniform", camera.getProjectionMatrix());
        Assets.CHUNK_SHADER.uploadMat4f("viewUniform", camera.getViewMatrix());

        Assets.BLOCK_SHEET.bind();

        regionRenderers.values().forEach(RegionRenderer::render);

        Assets.BLOCK_SHEET.unbind();
        Assets.CHUNK_SHADER.detach();
        framebuffer.unbind();

        ImGui.begin("Map", ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

        ImVec2 windowSize = getLargestSizeForViewport();
        ImVec2 windowPos = getCenteredPositionForViewport(windowSize);

        ImGui.setCursorPos(windowPos.x, windowPos.y);
        ImGui.image(framebuffer.getTextureId(), windowSize.x, windowSize.y, 0, 1, 1, 0);

        ImGui.end();
    }

    private static ImVec2 getLargestSizeForViewport() {
        ImVec2 windowSize = new ImVec2();
        ImGui.getContentRegionAvail(windowSize);
        windowSize.x -= ImGui.getScrollX();
        windowSize.y -= ImGui.getScrollY();

        float aspectWidth = windowSize.x;
        float aspectHeight = aspectWidth / Window.getTargetAspectRatio();

        if(aspectHeight > windowSize.y) {
            aspectHeight = windowSize.y;
            aspectWidth = aspectHeight * Window.getTargetAspectRatio();
        }

        return new ImVec2(aspectWidth, aspectHeight);
    }

    private static ImVec2 getCenteredPositionForViewport(ImVec2 aspectSize) {
        ImVec2 windowSize = new ImVec2();
        ImGui.getContentRegionAvail(windowSize);
        windowSize.x -= ImGui.getScrollX();
        windowSize.y -= ImGui.getScrollY();

        float x = (windowSize.x / 2.0f) - (aspectSize.x / 2.0f);
        float y = (windowSize.y / 2.0f) - (aspectSize.y / 2.0f);

        return new ImVec2(x + ImGui.getCursorPosX(), y + ImGui.getCursorPosY());
    }
}
