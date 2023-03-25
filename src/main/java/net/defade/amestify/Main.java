package net.defade.amestify;

import net.defade.amestify.graphics.Assets;
import net.defade.amestify.graphics.Window;
import net.defade.amestify.graphics.gui.ViewerGUI;

import java.io.IOException;
import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) throws IOException, URISyntaxException {
        Window.init(1920, 1080, "Amestify");
        Assets.init();

        Window.loop(new ViewerGUI());
    }
}
