package net.defade.amestify.graphics;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;

import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private static long TIME_STARTED;

    private static int width, height;
    private static String title;
    private static long windowPointer;

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
            Window.height = height;
            glfwSetWindowSize(window, width, height);
        });

        glfwMakeContextCurrent(windowPointer);
        glfwSwapInterval(1); // Enables V-Sync

        GL.createCapabilities();

        glViewport(0, 0, width, height);

        glDebugMessageControl(GL_DEBUG_SOURCE_API, GL_DEBUG_TYPE_OTHER, GL_DONT_CARE, 0x20071, false);
        GLUtil.setupDebugMessageCallback(System.err);
    }

    public static void loop() {
        TIME_STARTED = System.nanoTime();
        long beginTime = 0;
        long endTime;
        long deltaTime = -1;

        while (!glfwWindowShouldClose(windowPointer)) {
            glfwPollEvents();

            if (deltaTime >= 0) {
                glClearColor(0.4f, 0.4f, 0.7f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT);
            }

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

    private static long getTime() {
        return System.nanoTime() - TIME_STARTED;
    }
}
