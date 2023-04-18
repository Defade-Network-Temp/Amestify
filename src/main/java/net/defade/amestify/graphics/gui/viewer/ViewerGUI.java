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
import net.defade.amestify.loaders.anvil.RegionFile;
import net.defade.amestify.world.World;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.lang.Math;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class ViewerGUI extends GUI {
    private final DatabaseConnectorGUI databaseConnectorGUI = new DatabaseConnectorGUI();
    private final WorldLoaderGUI worldLoaderGUI = new WorldLoaderGUI();
    private final BiomeCreatorWindow biomeCreatorWindow = new BiomeCreatorWindow();
    private final BiomeSelectorWindow biomeSelectorWindow = new BiomeSelectorWindow();
    private boolean isViewDisabled = false;

    private final Framebuffer framebuffer = new Framebuffer(1920, 1080);

    private final ImVec2 viewportPos = new ImVec2();
    private final ImVec2 viewportSize = new ImVec2();
    private final Camera camera = new Camera();
    private Vector2f clickOrigin = null;
    private float resetCameraLerpTime = -1;
    private float cameraLerpZoom = -1;
    private Vector2f cameraLerpOrigin = null;

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

        ImVec2 windowSize = getLargestSizeForViewport();
        ImVec2 windowPos = getCenteredPositionForViewport(windowSize);

        ImGui.setCursorPos(windowPos.x, windowPos.y);

        viewportPos.set(getViewPortPos());
        viewportSize.set(windowSize);

        updateControllers(deltaTime);
        framebuffer.bind();

        glClearColor(1, 1, 1, 1);
        glClear(GL_COLOR_BUFFER_BIT);

        Assets.CHUNK_SHADER.attach();
        Assets.CHUNK_SHADER.uploadMat4f("projectionUniform", camera.getProjectionMatrix());
        Assets.CHUNK_SHADER.uploadMat4f("viewUniform", camera.getViewMatrix());
        uploadHighlightedBlock();

        Assets.BLOCK_SHEET.bind();

        renderRegions();

        Assets.BLOCK_SHEET.unbind();
        Assets.CHUNK_SHADER.detach();

        renderGrid();

        framebuffer.unbind();

        ImGui.image(framebuffer.getTextureId(), windowSize.x, windowSize.y, 0, 1, 1, 0);
        ImGui.end();
    }

    private void updateControllers(float deltaTime) {
        if(resetCameraLerpTime >= 0) {
            resetCameraLerpTime = clamp(resetCameraLerpTime + (deltaTime * 2), 0, 1);
            camera.setZoom(lerp(cameraLerpZoom, 1, resetCameraLerpTime));
            camera.adjustProjection();

            float x = lerp(cameraLerpOrigin.x, 0, resetCameraLerpTime);
            float y = lerp(cameraLerpOrigin.y, 0, resetCameraLerpTime);
            camera.getPosition().set(x, y);
        }

        if(resetCameraLerpTime == 1) {
            resetCameraLerpTime = -1;
        }

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
                resetCameraLerpTime = 0;
                cameraLerpZoom = camera.getZoom();
                cameraLerpOrigin = new Vector2f(camera.getPosition());
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

    private void uploadHighlightedBlock() {
        // gl_FragCoord is in screen space, so we need to convert it to viewport space

        Vector2f start = worldCoordsToViewport(new Vector2f((float) (Math.floor(getViewportOrthoX() / 16) * 16), (float) (Math.floor(getViewportOrthoY() / 16) * 16)));
        Vector2f end = worldCoordsToViewport(new Vector2f((float) Math.floor(getViewportOrthoX() / 16) * 16 + 16, (float) Math.floor(getViewportOrthoY() / 16) * 16 + 16));

        Assets.CHUNK_SHADER.uploadVec4f("highlightedBlock", start.x, start.y, end.x, end.y);
    }

    private Vector2f worldCoordsToViewport(Vector2f worldCoords) {
        Matrix4f projectionMatrix = new Matrix4f(camera.getProjectionMatrix());
        Matrix4f viewMatrix = new Matrix4f(camera.getViewMatrix());

        Vector4f clipSpacePos = projectionMatrix.mul(viewMatrix).transform(new Vector4f(worldCoords.x, worldCoords.y, 0, 1));
        Vector3f ndcSpacePos = new Vector3f(
                clipSpacePos.x / clipSpacePos.w,
                clipSpacePos.y / clipSpacePos.w,
                clipSpacePos.z / clipSpacePos.w
        );

        return new Vector2f((ndcSpacePos.x + 1.0f) / 2.0f * framebuffer.getWidth(),
                (ndcSpacePos.y + 1.0f) / 2.0f * framebuffer.getHeight());
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

    private static ImVec2 getViewPortPos() {
        ImVec2 topLeft = new ImVec2();
        ImGui.getCursorScreenPos(topLeft);
        topLeft.x -= ImGui.getScrollX();
        topLeft.y -= ImGui.getScrollY();

        return topLeft;
    }

    private static float lerp(float start, float end, float t) {
        return start + t * (end - start);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
