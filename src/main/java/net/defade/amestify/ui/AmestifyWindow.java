package net.defade.amestify.ui;

import net.defade.amestify.database.MongoConnector;
import net.defade.amestify.ui.gui.MongoDbLoginGUI;
import javax.swing.JFrame;
import java.awt.Dimension;

public class AmestifyWindow extends JFrame {
    private static final Dimension MIN_DIMENSION = new Dimension(1280, 720);

    private final MongoConnector mongoConnector = new MongoConnector();

    public AmestifyWindow() {
        super("Amestify");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(MIN_DIMENSION);
        setMinimumSize(MIN_DIMENSION);
        setVisible(true);

        setContentPane(new MongoDbLoginGUI(this, mongoConnector));
        pack();

        Runtime.getRuntime().addShutdownHook(new Thread(mongoConnector::disconnect));
    }
}
