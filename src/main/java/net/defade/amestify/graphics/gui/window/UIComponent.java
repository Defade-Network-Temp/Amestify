package net.defade.amestify.graphics.gui.window;

public abstract class UIComponent {
    public abstract void render();

    public boolean isDisabledWhenNoMapLoaded() {
        return true;
    }
}
