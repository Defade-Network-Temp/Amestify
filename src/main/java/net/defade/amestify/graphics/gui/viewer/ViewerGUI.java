package net.defade.amestify.graphics.gui.viewer;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.defade.amestify.control.MouseListener;
import net.defade.amestify.graphics.Assets;
import net.defade.amestify.graphics.Camera;
import net.defade.amestify.graphics.Framebuffer;
import net.defade.amestify.graphics.Window;
import net.defade.amestify.graphics.gui.GUI;
import net.defade.amestify.graphics.gui.renderer.ShapeRenderer;
import net.defade.amestify.loaders.anvil.RegionFile;
import net.defade.amestify.world.World;
import net.defade.amestify.world.biome.Biome;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import java.lang.Math;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class ViewerGUI extends GUI {
    private final Framebuffer framebuffer = new Framebuffer(1920, 1080);
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();

    private final ImVec2 viewportPos = new ImVec2();
    private final ImVec2 viewportSize = new ImVec2();
    private final Camera camera = new Camera();
    private Vector2f clickOrigin = null;
    private final Vector2i hoveredBlock = new Vector2i(0, 0);

    private final DatabaseConnectorGUI databaseConnectorGUI = new DatabaseConnectorGUI();
    private final WorldLoaderGUI worldLoaderGUI = new WorldLoaderGUI();
    private final BiomeCreatorWindow biomeCreatorWindow = new BiomeCreatorWindow();
    private final BiomeSelectorWindow biomeSelectorWindow = new BiomeSelectorWindow();

    private boolean isViewDisabled = false;

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
                setWorld(worldLoaderGUI.getWorld());
            }
        }

        biomeCreatorWindow.render();
        biomeSelectorWindow.render();
        renderTooltipInfo();

        renderMap(deltaTime);

        enableView();

        ImGui.end(); // End the dockspace
    }

    @Override
    public boolean usesImGui() {
        return true;
    }

    private void setWorld(World world) {
        this.world = world;
        biomeCreatorWindow.setWorld(world);
        biomeSelectorWindow.setWorld(world);
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

    private void renderMap(float deltaTime) {
        ImGui.begin("Map", ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

        adjustProjectionSize();

        updateControllers(deltaTime);
        framebuffer.bind();

        glClearColor(1, 1, 1, 1);
        glClear(GL_COLOR_BUFFER_BIT);

        Assets.CHUNK_SHADER.attach();
        Assets.CHUNK_SHADER.uploadMat4f("projectionUniform", camera.getProjectionMatrix());
        Assets.CHUNK_SHADER.uploadMat4f("viewUniform", camera.getViewMatrix());
        Assets.CHUNK_SHADER.uploadBoolean("displayBiomeColor", biomeSelectorWindow.shouldShowBiomeLayer());
        uploadHighlightedElements();

        Assets.BLOCK_SHEET.bind();
        if(world != null) world.getBiomeColorLayer().bind();

        renderRegions();

        if(world != null) world.getBiomeColorLayer().unbind();
        Assets.BLOCK_SHEET.unbind();
        Assets.CHUNK_SHADER.detach();

        shapeRenderer.render(camera);

        renderGrid();

        framebuffer.unbind();

        ImGui.image(framebuffer.getTextureId(), viewportSize.x, viewportSize.y, 0, 1, 1, 0);
        ImGui.end();
    }

    private void adjustProjectionSize() {
        ImVec2 windowSize = new ImVec2();
        ImGui.getContentRegionAvail(windowSize);
        ImVec2 windowPos = getCenteredPositionForViewport(windowSize);

        ImGui.setCursorPos(windowPos.x, windowPos.y);

        viewportPos.set(getViewPortPos());
        viewportSize.set(windowSize);

        float newProjectionX = ((16 * 40) * windowSize.x) / framebuffer.getWidth();
        float newProjectionY = ((16 * 21) * windowSize.y) / framebuffer.getHeight();
        camera.getProjectionSize().set(newProjectionX, newProjectionY);
        camera.adjustProjection();
    }

    private void updateControllers(float deltaTime) {
        camera.update(deltaTime);
        hoveredBlock.set((int) Math.floor(getViewportOrthoX() / 16), (int) Math.floor(getViewportOrthoY() / 16));

        if(isViewDisabled) return;

        if(isMouseInViewport()) {
            if (MouseListener.getScrollY() != 0.0f) {
                Vector2f cameraCenter = new Vector2f(camera.getPosition()).add(
                        new Vector2f(
                                camera.getPosition().x + camera.getProjectionSize().x * camera.getZoom(),
                                camera.getPosition().y + camera.getProjectionSize().y * camera.getZoom()
                        )
                ).div(2);

                float addValue = (float) Math.pow(Math.abs(MouseListener.getScrollY() * 0.1f), 1 / camera.getZoom());
                addValue *= -MouseListener.getScrollY();
                camera.addZoom(addValue);
                camera.adjustProjection();

                if(clickOrigin == null) { // Only adjust the camera if the user is not dragging
                    Vector2f newCameraCenter = new Vector2f(camera.getPosition()).add(
                            new Vector2f(
                                    camera.getPosition().x + camera.getProjectionSize().x * camera.getZoom(),
                                    camera.getPosition().y + camera.getProjectionSize().y * camera.getZoom()
                            )
                    ).div(2);

                    Vector2f delta = new Vector2f(cameraCenter).sub(newCameraCenter);
                    camera.getPosition().add(delta);
                }
            }

            if (MouseListener.isMouseButtonDown(GLFW_MOUSE_BUTTON_MIDDLE)) { // TODO: Change to key
                camera.reset();
            }
        }

        if (MouseListener.isMouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
            if(clickOrigin == null && isMouseInViewport()) {
                clickOrigin = new Vector2f(getViewportOrthoX(), getViewportOrthoY());
            }

            if(clickOrigin != null) {
                Vector2f mousePos = new Vector2f(getViewportOrthoX(), getViewportOrthoY());
                Vector2f delta = new Vector2f(mousePos).sub(this.clickOrigin);
                camera.getPosition().sub(delta);
            }
        } else {
            clickOrigin = null;
        }
    }

    private void uploadHighlightedElements() {
        if(world != null && isMouseInViewport()) {
            Biome highlightedBiome = world.getBiomeAt(hoveredBlock.x, hoveredBlock.y);
            Assets.CHUNK_SHADER.uploadFloat("highlightedBiome", highlightedBiome != null ? highlightedBiome.id() : -1);
        }

        shapeRenderer.addSquare(hoveredBlock.x, hoveredBlock.y, hoveredBlock.x + 1, hoveredBlock.y + 1, 0xAA282828);
    }

    private void renderRegions() {
        if(world == null) return;

        int minSeenRegionX = (int) Math.floor(camera.getPosition().x / 16 / 16 / 32);
        int maxSeenRegionX = (int) Math.floor((camera.getPosition().x + camera.getProjectionSize().x * camera.getZoom()) / 16 / 16 / 32);
        int minSeenRegionZ = (int) Math.floor(camera.getPosition().y / 16 / 16 / 32);
        int maxSeenRegionZ = (int) Math.floor((camera.getPosition().y + camera.getProjectionSize().y * camera.getZoom()) / 16 / 16 / 32);

        for (RegionFile region : world.getRegions()) {
            if(region.getRegionPos().x() >= minSeenRegionX && region.getRegionPos().x() <= maxSeenRegionX && region.getRegionPos().z() >= minSeenRegionZ && region.getRegionPos().z() <= maxSeenRegionZ) {
                region.getRenderer().render();
            }
        }
    }

    private float getViewportOrthoX() {
        float currentX = (float) (MouseListener.getX() - viewportPos.x);
        currentX = (currentX / viewportSize.x) * 2.0f - 1.0f;
        Vector4f tmp = new Vector4f(currentX, 0, 0, 1);

        Matrix4f viewProjection = new Matrix4f();
        camera.getInverseView().mul(camera.getInverseProjection(), viewProjection);
        tmp.mul(viewProjection);
        currentX = tmp.x;

        return currentX;
    }

    private float getViewportOrthoY() {
        float currentY = (float) (MouseListener.getY() - viewportPos.y);
        currentY = -((currentY / viewportSize.y) * 2.0f - 1.0f);
        Vector4f tmp = new Vector4f(0, currentY, 0, 1);

        Matrix4f viewProjection = new Matrix4f();
        camera.getInverseView().mul(camera.getInverseProjection(), viewProjection);
        tmp.mul(viewProjection);
        currentY = tmp.y;

        return currentY;
    }

    private boolean isMouseInViewport() {
        return MouseListener.getX() >= viewportPos.x && MouseListener.getX() <= viewportPos.x + viewportSize.x &&
                MouseListener.getY() >= viewportPos.y && MouseListener.getY() <= viewportPos.y + viewportSize.y &&
                ImGui.isWindowHovered();
    }

    private void renderGrid() {
        Vector2f projectionSize = new Vector2f(camera.getZoom()).mul(camera.getProjectionSize());

        int areaSeen = (int) ((projectionSize.x / 16) * (projectionSize.y / 16));
        int gridSize = 16; // Block grid
        if(areaSeen >= 9000 && areaSeen < 660000) {
            gridSize = 16 * 16; // Chunk grid
        } else if(areaSeen >= 660000) {
            gridSize = 32 * 16 * 16; // Region grid
        }

        int firstX = (int) Math.floor(camera.getPosition().x / gridSize) * gridSize;
        int firstY = (int) Math.floor(camera.getPosition().y / gridSize) * gridSize;

        int endX = firstX + (int) Math.ceil((projectionSize.x / gridSize) + 1) * gridSize;
        int endY = firstY + (int) Math.ceil((projectionSize.y / gridSize) + 1) * gridSize;
        int xLines = (int) projectionSize.x / gridSize + 2;
        int yLines = (int) projectionSize.y / gridSize + 2;

        Assets.GRID_SHADER.attach();
        Assets.GRID_SHADER.uploadMat4f("projection", camera.getProjectionMatrix());
        Assets.GRID_SHADER.uploadMat4f("view", camera.getViewMatrix());
        Assets.GRID_SHADER.uploadVec4i("coords", firstX, firstY, endX, endY);
        Assets.GRID_SHADER.uploadInt("xLines", xLines);
        Assets.GRID_SHADER.uploadInt("gridSize", gridSize);


        glDrawArraysInstanced(GL_LINES, 0, 2, xLines + yLines);
        Assets.GRID_SHADER.detach();
    }

    private void renderTooltipInfo() {
        int regionX = (int) Math.floor(hoveredBlock.x / 512f);
        int regionZ = (int) Math.floor(hoveredBlock.y / 512f);

        Biome selectedBiome = world == null ? null : world.getBiomeAt(hoveredBlock.x, hoveredBlock.y);
        String[] tooltipText = new String[] {
                hoveredBlock.x + ", " + hoveredBlock.y,
                "Biome: " + (selectedBiome == null ? "None" : selectedBiome.name().asString()),
                "Region: r." + regionX + "." + regionZ + ".mca"
        };

        int tooltipSizeX = 0;
        for (String text : tooltipText) {
            float size = ImGui.calcTextSize(text).x;
            if(size > tooltipSizeX) {
                tooltipSizeX = (int) size;
            }
        }

        tooltipSizeX += (ImGui.getStyle().getWindowPaddingX() * 2);

        if(tooltipSizeX < 225) tooltipSizeX = 225; // Put a minimum size that is big enough
        // so the tooltip doesn't constantly resize

        ImGui.setNextWindowSize(tooltipSizeX, 0);
        ImGui.setNextWindowPos(viewportPos.x + viewportSize.x - tooltipSizeX - (ImGui.getStyle().getWindowPaddingX()), viewportPos.y + ImGui.getStyle().getWindowPaddingY());
        ImGui.beginTooltip();
        for (String text : tooltipText) {
            ImGui.text(text);
        }
        ImGui.endTooltip();
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

    private static ImVec2 getViewPortPos() {
        ImVec2 topLeft = new ImVec2();
        ImGui.getCursorScreenPos(topLeft);
        topLeft.x -= ImGui.getScrollX();
        topLeft.y -= ImGui.getScrollY();

        return topLeft;
    }
}
