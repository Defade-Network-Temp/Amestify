package net.defade.amestify;

import com.formdev.flatlaf.FlatDarkLaf;
import net.defade.amestify.ui.AmestifyWindow;

public class Main {
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        new AmestifyWindow();
    }
}
