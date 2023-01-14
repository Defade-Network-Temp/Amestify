package net.defade.amestify;

import com.formdev.flatlaf.FlatDarkLaf;
import net.defade.amestify.ui.AmestifyWindow;
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
            return;
        }

        FlatDarkLaf.setup();
        new AmestifyWindow();
    }
}
