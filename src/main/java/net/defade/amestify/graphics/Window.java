package net.defade.amestify.graphics;

import net.defade.amestify.graphics.gui.GUI;
import net.defade.amestify.graphics.gui.ImGUILayer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private static long TIME_STARTED;

    private static int width, height;
    private static String title;
    private static long windowPointer;

    private static final ImGUILayer imGUILayer = new ImGUILayer();

    private static GUI gui;

    public static void init(int width, int height, String title) {
        Thread.currentThread().setName("Render Thread");

        Window.width = width;
        Window.height = height;
        Window.title = title;

        GLFWErrorCallback.createPrint(System.err).set();

        if(!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW.");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);

        windowPointer = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowPointer == NULL) {
            throw new RuntimeException("Failed to create the GLFW window.");
        }

        glfwSetWindowSizeCallback(windowPointer, (window, widthCallback, heightCallback) -> {
            Window.width = widthCallback;
            Window.height = heightCallback;
            glfwSetWindowSize(window, width, height);
        });

        glfwMakeContextCurrent(windowPointer);
        glfwSwapInterval(1); // Enables V-Sync

        GL.createCapabilities();

        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        glViewport(0, 0, width, height);
        imGUILayer.initImGui();

        glDebugMessageControl(GL_DEBUG_SOURCE_API, GL_DEBUG_TYPE_OTHER, GL_DONT_CARE, 0x20071, false);
        GLUtil.setupDebugMessageCallback(System.err);
    }

    public static void loop(GUI startGUI) {
        gui = startGUI;
        TIME_STARTED = System.nanoTime();
        long beginTime = 0;
        long endTime;
        long deltaTime = -1;

        while (!glfwWindowShouldClose(windowPointer)) {
            glfwPollEvents();

            if (deltaTime >= 0) {
                gui.render(deltaTime);
            }

            imGUILayer.update(deltaTime / 1e9f); // ImGUI expects a delta time in seconds

            glfwSwapBuffers(windowPointer);

            endTime = getTime();
            deltaTime = endTime - beginTime;
            beginTime = endTime;
        }

        glfwFreeCallbacks(windowPointer);
        glfwDestroyWindow(windowPointer);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static long getWindowPointer() {
        return windowPointer;
    }

    public static GUI getGUI() {
        return gui;
    }

    private static long getTime() {
        return System.nanoTime() - TIME_STARTED;
    }

    public static float getTargetAspectRatio() {
        return 16f / 9f;
    }
}
