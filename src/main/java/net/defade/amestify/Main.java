package net.defade.amestify;

import com.formdev.flatlaf.FlatDarkLaf;
import net.defade.amestify.ui.AmestifyWindow;
import net.minestom.server.MinecraftServer;

public class Main {
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        MinecraftServer.init(); // Init the overworld dimension.
        new AmestifyWindow();
    }
}
