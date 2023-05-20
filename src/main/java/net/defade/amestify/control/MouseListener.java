package net.defade.amestify.control;

import org.lwjgl.glfw.GLFW;

public class MouseListener {
    private static double scrollX = 0;
    private static double scrollY = 0;
    private static double xPos = 0;
    private static double yPos = 0;
    private static final boolean[] buttonsPressed = new boolean[9];
    private static boolean isDragging = false;

    public static void mouseButtonCallback(int button, int action) {
        if (action == GLFW.GLFW_PRESS) {
            buttonsPressed[button] = true;
        } else if (action == GLFW.GLFW_RELEASE) {
            buttonsPressed[button] = false;
            isDragging = false;
        }
    }

    public static void mousePosCallback(double x, double y) {
        xPos = x;
        yPos = y;
        isDragging = buttonsPressed[0] || buttonsPressed[1] || buttonsPressed[2];
    }

    public static void scrollCallback(double x, double y) {
        scrollX = x;
        scrollY = y;
    }

    public static void endFrame() {
        scrollX = 0;
        scrollY = 0;
    }

    public static double getX() {
        return xPos;
    }

    public static double getY() {
        return yPos;
    }

    public static double getScrollX() {
        return scrollX;
    }

    public static double getScrollY() {
        return scrollY;
    }

    public static boolean isDragging() {
        return isDragging;
    }

    public static boolean isMouseButtonDown(int button) {
        return buttonsPressed[button];
    }
}
