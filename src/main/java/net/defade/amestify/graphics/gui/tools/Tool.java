package net.defade.amestify.graphics.gui.tools;

import net.defade.amestify.graphics.gui.renderer.ShapeRenderer;
import net.defade.amestify.graphics.rendering.texture.Texture;

public abstract class Tool {
    private final String name;
    private final Texture icon;

    private boolean active = false;

    protected Tool(String name, Texture icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public Texture getIcon() {
        return icon;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public abstract void updateClick(boolean pressed);

    public void renderShapes(ShapeRenderer shapeRenderer) {

    }
}
