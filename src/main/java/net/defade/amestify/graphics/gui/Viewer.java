package net.defade.amestify.graphics.gui;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.defade.amestify.control.MouseListener;
import net.defade.amestify.database.MongoConnector;
import net.defade.amestify.graphics.gui.dialog.Dialog;
import net.defade.amestify.graphics.gui.window.BiomeCreatorUI;
import net.defade.amestify.graphics.gui.window.BiomeSelectorUI;
import net.defade.amestify.graphics.gui.window.UIComponent;
import net.defade.amestify.utils.Utils;
import net.defade.amestify.world.MapViewerRegion;
import net.defade.amestify.graphics.gui.dialog.AmethystSaveFileGUI;
import net.defade.amestify.graphics.gui.dialog.DatabaseConnectorDialog;
import net.defade.amestify.graphics.gui.dialog.WorldLoaderDialog;
import net.defade.amestify.graphics.rendering.Assets;
import net.defade.amestify.graphics.rendering.Camera;
import net.defade.amestify.graphics.rendering.Framebuffer;
import net.defade.amestify.graphics.rendering.Window;
import net.defade.amestify.graphics.gui.renderer.ShapeRenderer;
import net.defade.amestify.world.MapViewerWorld;
import net.defade.amestify.world.biome.Biome;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import java.lang.Math;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class Viewer {
    private final MongoConnector mongoConnector = new MongoConnector();

    private final Framebuffer framebuffer = new Framebuffer(1920, 1080);
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();

    private final ImVec2 viewportPos = new ImVec2();
    private final ImVec2 viewportSize = new ImVec2();
    private final Camera camera = new Camera();
    private Vector2f clickOrigin = null;
    private final Vector2i hoveredBlock = new Vector2i(0, 0);

    private Vector2i selectedRegionOrigin = null;

    private final Map<Class<? extends UIComponent>, UIComponent> uiComponents = new HashMap<>();
    private final Map<Class<? extends Dialog>, Dialog> dialogs = new HashMap<>();

    private boolean isViewDisabled = false;

    private MapViewerWorld mapViewerWorld;

    public Viewer() {
        uiComponents.put(BiomeCreatorUI.class, new BiomeCreatorUI(this));
        uiComponents.put(BiomeSelectorUI.class, new BiomeSelectorUI(this));

        dialogs.put(DatabaseConnectorDialog.class, new DatabaseConnectorDialog(this));
        dialogs.put(WorldLoaderDialog.class, new WorldLoaderDialog(this));
        dialogs.put(AmethystSaveFileGUI.class, new AmethystSaveFileGUI(this));
    }

    public void render(float deltaTime) {
        setupDockspace();

        dialogs.values().forEach(dialog -> {
            if(dialog.isEnabled()) {
                dialog.render();

                disableView();
            }
        });

        renderMenuBar();

        if(mapViewerWorld == null) disableView();

        uiComponents.values().forEach(UIComponent::render);

        renderTooltipInfo();

        renderMap(deltaTime);

        enableView();

        ImGui.end(); // End the dockspace
    }

    public MongoConnector getMongoConnector() {
        return mongoConnector;
    }

    public MapViewerWorld getMapViewerWorld() {
        return mapViewerWorld;
    }

    public void setMapViewerWorld(MapViewerWorld mapViewerWorld) {
        this.mapViewerWorld = mapViewerWorld;
    }

    private <T extends UIComponent> T getUIComponent(Class<T> clazz) {
        UIComponent uiComponent = uiComponents.get(clazz);
        if(uiComponent == null) {
            throw new RuntimeException("UIComponent " + clazz.getName() + " is not registered");
        }

        if (!clazz.isAssignableFrom(uiComponent.getClass())) {
            throw new RuntimeException("UIComponent " + uiComponent.getClass().getName() +
                    " is not assignable from " + clazz.getName());
        } else {
            return clazz.cast(uiComponent);
        }
    }

    private <T extends Dialog> T getDialog(Class<T> clazz) {
        Dialog dialog = dialogs.get(clazz);
        if(dialog == null) {
            throw new RuntimeException("Dialog " + clazz.getName() + " is not registered");
        }

        if (!clazz.isAssignableFrom(dialog.getClass())) {
            throw new RuntimeException("Dialog " + dialog.getClass().getName() +
                    " is not assignable from " + clazz.getName());
        } else {
            return clazz.cast(dialog);
        }
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

    private void renderMenuBar() {
        if(ImGui.beginMenuBar()) {
            if(ImGui.beginMenu("File")) {
                if(ImGui.menuItem("Open world")) {
                    getDialog(WorldLoaderDialog.class).enable();
                }
                ImGui.endMenu();
            }

            if(mapViewerWorld == null) ImGui.beginDisabled(true);
            if(ImGui.beginMenu("Save")) {
                if(ImGui.menuItem("Save to amethyst file")) {
                    if(mapViewerWorld != null) getDialog(AmethystSaveFileGUI.class).enable();
                }

                if(!mongoConnector.isConnected()) ImGui.beginDisabled(true);
                if(ImGui.menuItem("Save to database")) {
                    // TODO
                }
                if(!mongoConnector.isConnected()) {
                    ImGui.endDisabled();
                    if(ImGui.isItemHovered(ImGuiHoveredFlags.AllowWhenDisabled)) {
                        ImGui.beginTooltip();
                        ImGui.textColored(255,185, 0, 255, "You must be connected to a database to do this!");
                        ImGui.endTooltip();
                    }
                }

                ImGui.endMenu();
            }
            if(mapViewerWorld == null) {
                ImGui.endDisabled();
                if(ImGui.isItemHovered(ImGuiHoveredFlags.AllowWhenDisabled)) {
                    ImGui.beginTooltip();
                    ImGui.textColored(255,185, 0, 255, "You must open a world to do this!");
                    ImGui.endTooltip();
                }
            }

            if(ImGui.beginMenu("Database")) {
                if(ImGui.menuItem("Connect to a database")) {
                    getDialog(DatabaseConnectorDialog.class).enable();
                }

                boolean isDisabled = !mongoConnector.isConnected();
                if(isDisabled) ImGui.beginDisabled(true);
                if(ImGui.menuItem("Disconnect from database")) {
                    mongoConnector.disconnect();
                }
                if(isDisabled) ImGui.endDisabled();

                ImGui.endMenu();
            }

            ImGui.endMenuBar();
        }
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

        glClearColor(43 / 255f, 42 / 255f, 51 / 255f, 1);
        glClear(GL_COLOR_BUFFER_BIT);

        Assets.CHUNK_SHADER.attach();
        Assets.CHUNK_SHADER.uploadMat4f("projectionUniform", camera.getProjectionMatrix());
        Assets.CHUNK_SHADER.uploadMat4f("viewUniform", camera.getViewMatrix());
        Assets.CHUNK_SHADER.uploadBoolean("displayBiomeColor", getUIComponent(BiomeSelectorUI.class).shouldShowBiomeLayer());
        uploadHighlightedElements();

        Assets.BLOCK_SHEET.bind();
        if(mapViewerWorld != null) mapViewerWorld.getBiomeColorLayer().bind();

        renderRegions();

        if(mapViewerWorld != null) mapViewerWorld.getBiomeColorLayer().unbind();
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

        if(MouseListener.isMouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT)) {
            if(selectedRegionOrigin == null && isMouseInViewport()) {
                selectedRegionOrigin = new Vector2i(hoveredBlock);
            }
        } else if (selectedRegionOrigin != null) {
            int minX = Math.min(selectedRegionOrigin.x, hoveredBlock.x);
            int maxX = Math.max(selectedRegionOrigin.x, hoveredBlock.x);
            int minZ = Math.min(selectedRegionOrigin.y, hoveredBlock.y);
            int maxZ = Math.max(selectedRegionOrigin.y, hoveredBlock.y);

            int minRegionX = (int) Math.floor((double) minX / 512);
            int maxRegionX = (int) Math.floor((double) maxX / 512);
            int minRegionZ = (int) Math.floor((double) minZ / 512);
            int maxRegionZ = (int) Math.floor((double) maxZ / 512);
            for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
                for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                    MapViewerRegion mapViewerRegion = mapViewerWorld.getRegion(regionX, regionZ);
                    if(mapViewerRegion == null) continue;

                    int minRegionBlockX = Math.max(minX, regionX * 512);
                    int maxRegionBlockX = Math.min(maxX, regionX * 512 + 511);
                    int minRegionBlockZ = Math.max(minZ, regionZ * 512);
                    int maxRegionBlockZ = Math.min(maxZ, regionZ * 512 + 511);

                    for (int x = minRegionBlockX; x <= maxRegionBlockX; x++) {
                        for (int z = minRegionBlockZ; z <= maxRegionBlockZ; z++) {
                            mapViewerRegion.setBiome(x, z, getUIComponent(BiomeSelectorUI.class).getSelectedBiome());
                        }
                    }

                    mapViewerRegion.getRenderer().updateMesh();
                }
            }

            selectedRegionOrigin = null;
        }
    }

    private void uploadHighlightedElements() {
        if(mapViewerWorld != null && isMouseInViewport()) {
            Biome highlightedBiome = mapViewerWorld.getBiomeAt(hoveredBlock.x, hoveredBlock.y);
            Assets.CHUNK_SHADER.uploadFloat("highlightedBiome", highlightedBiome != null ? highlightedBiome.id() : -1);
            Assets.CHUNK_SHADER.uploadFloat("biomeHighlightColorMultiplier",
                    Math.max(0.5f, Utils.lerp(0.75f, 0.5f, camera.getZoom() / (Camera.MAX_ZOOM / 3))));
        }

        shapeRenderer.addSquare(hoveredBlock.x, hoveredBlock.y, hoveredBlock.x + 1, hoveredBlock.y + 1, 0xAA282828);

        if(selectedRegionOrigin != null) {
            shapeRenderer.addSquare(selectedRegionOrigin.x, selectedRegionOrigin.y, hoveredBlock.x + 1, hoveredBlock.y, 0x99C46200);
        }
    }

    private void renderRegions() {
        if(mapViewerWorld == null) return;

        int minSeenRegionX = (int) Math.floor(camera.getPosition().x / 16 / 16 / 32);
        int maxSeenRegionX = (int) Math.floor((camera.getPosition().x + camera.getProjectionSize().x * camera.getZoom()) / 16 / 16 / 32);
        int minSeenRegionZ = (int) Math.floor(camera.getPosition().y / 16 / 16 / 32);
        int maxSeenRegionZ = (int) Math.floor((camera.getPosition().y + camera.getProjectionSize().y * camera.getZoom()) / 16 / 16 / 32);

        for (MapViewerRegion region : mapViewerWorld.getRegions()) {
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

        Biome selectedBiome = mapViewerWorld == null ? null : mapViewerWorld.getBiomeAt(hoveredBlock.x, hoveredBlock.y);
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
