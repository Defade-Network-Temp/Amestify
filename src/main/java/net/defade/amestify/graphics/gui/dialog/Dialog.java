package net.defade.amestify.graphics.gui.dialog;

public abstract class Dialog {
    private boolean enabled = false;

    public abstract void render();

    public void enable() {
        if(!isEnabled()) reset();
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected void reset() { }
}
