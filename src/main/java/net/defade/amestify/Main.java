package net.defade.amestify;

import net.defade.amestify.graphics.Window;
import net.defade.amestify.world.Block;
import net.defade.amestify.world.biome.Biome;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            Block.init();
            Biome.init();
        } catch (IOException exception) {
            exception.printStackTrace();
            System.err.println("Couldn't load block or biome data.");
        }

        Window.init(1920, 1080, "Amestify");
        Window.loop();
    }
}
