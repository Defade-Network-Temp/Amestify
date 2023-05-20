package net.defade.amestify;

import net.defade.amestify.graphics.rendering.Assets;
import net.defade.amestify.graphics.rendering.Window;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Window.init(1920, 1080, "Amestify");
        Assets.init();

        Window.loop();
    }
}
