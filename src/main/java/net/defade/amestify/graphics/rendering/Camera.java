package net.defade.amestify.graphics.rendering;

import net.defade.amestify.utils.Utils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Camera {
    public static final float MAX_ZOOM = 45f;

    private final Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private final Matrix4f inverseProjection;
    private final Matrix4f inverseView;
    private final Vector2f position = new Vector2f();
    private final Vector2f projectionSize = new Vector2f(16 * 40, 16 * 21);

    private float zoom = 1.0f;
    private float resetLerpTime = -1;
    private float zoomLerp = -1;
    private Vector2f lerpOrigin = null;

    public Camera() {
        this.projectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.inverseProjection = new Matrix4f();
        this.inverseView = new Matrix4f();
        adjustProjection();
    }

    public void update(float deltaTime) {
        if(resetLerpTime >= 0) {
            resetLerpTime = (float) Utils.clamp(0, 1, resetLerpTime + (deltaTime * 2));
            setZoom(Utils.lerp(zoomLerp, 1, resetLerpTime));
            adjustProjection();

            float x = Utils.lerp(lerpOrigin.x, 0, resetLerpTime);
            float y = Utils.lerp(lerpOrigin.y, 0, resetLerpTime);
            getPosition().set(x, y);
        }

        if(resetLerpTime == 1) {
            resetLerpTime = -1;
        }
    }

    public void reset() {
        resetLerpTime = 0;
        zoomLerp = zoom;
        lerpOrigin = new Vector2f(position);
    }

    public void adjustProjection() {
        projectionMatrix.identity();
        projectionMatrix.ortho(0, projectionSize.x * zoom, 0, projectionSize.y * zoom, 0, 100.0f);
        projectionMatrix.invert(inverseProjection);
    }

    public Matrix4f getViewMatrix() {
        Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f);
        Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);

        this.viewMatrix.identity();
        this.viewMatrix = viewMatrix.lookAt(new Vector3f(position.x, position.y, 50), cameraFront.add(position.x, position.y, 0.0f), cameraUp);

        this.viewMatrix.invert(inverseView);

        return this.viewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }

    public Vector2f getPosition() {
        return position;
    }

    public Matrix4f getInverseProjection() {
        return inverseProjection;
    }

    public Matrix4f getInverseView() {
        return inverseView;
    }

    public Vector2f getProjectionSize() {
        return projectionSize;
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = Math.min(zoom, MAX_ZOOM);
    }

    public void addZoom(float value) {
        this.zoom = Math.min(zoom + value, MAX_ZOOM);
    }
}
