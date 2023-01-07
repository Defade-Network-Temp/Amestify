package net.defade.amestify.ui.gui;

import org.jdesktop.swingx.prompt.PromptSupport;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.NumberFormatter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.text.NumberFormat;

public class MongoDbLoginGUI extends JPanel {
    private final JTextField hostField = new JTextField();
    private final JFormattedTextField portField = new JFormattedTextField();
    private final JTextField databaseField = new JTextField();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton buttonField = new JButton();

    public MongoDbLoginGUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Box globalBox = Box.createVerticalBox();
        Box credentialsBox = Box.createVerticalBox();

        PromptSupport.init("Host", null, null, hostField);
        hostField.setPreferredSize(new Dimension(200 - 64 - 4, 48));
        hostField.setMaximumSize(new Dimension(200 - 64 - 4, 48));
        hostField.setAlignmentX(JTextField.CENTER_ALIGNMENT);

        PromptSupport.init("Port", null, null, portField);
        portField.setFormatterFactory(createIntegerFormatter());
        portField.setPreferredSize(new Dimension(64, 48));
        portField.setMaximumSize(new Dimension(64, 48));
        portField.setAlignmentX(JTextField.CENTER_ALIGNMENT);

        PromptSupport.init("Authentication Database", null, null, databaseField);
        databaseField.setPreferredSize(new Dimension(200, 48));
        databaseField.setMaximumSize(new Dimension(200, 48));
        databaseField.setAlignmentX(JTextField.CENTER_ALIGNMENT);

        PromptSupport.init("Username", null, null, usernameField);
        usernameField.setPreferredSize(new Dimension(200, 48));
        usernameField.setMaximumSize(new Dimension(200, 48));
        usernameField.setAlignmentX(JTextField.CENTER_ALIGNMENT);

        PromptSupport.init("Password", null, null, passwordField);
        passwordField.setPreferredSize(new Dimension(200, 48));
        passwordField.setMaximumSize(new Dimension(200, 48));
        passwordField.setAlignmentX(JTextField.CENTER_ALIGNMENT);

        buttonField.setText("Connect");
        buttonField.setPreferredSize(new Dimension(200, 48));
        buttonField.setMaximumSize(new Dimension(200, 48));
        buttonField.setAlignmentX(JTextField.CENTER_ALIGNMENT);

        credentialsBox.add(stickHorizontally(hostField, createRigidArea(4, 0), portField));
        credentialsBox.add(createRigidArea(0, 4));
        credentialsBox.add(databaseField);
        credentialsBox.add(createRigidArea(0, 4));
        credentialsBox.add(usernameField);
        credentialsBox.add(createRigidArea(0, 4));
        credentialsBox.add(passwordField);

        credentialsBox.setBorder(
                new TitledBorder(
                        new LineBorder(new Color(127, 127, 127), 3, true),
                        "Credentials", TitledBorder.DEFAULT_JUSTIFICATION,
                        TitledBorder.DEFAULT_POSITION,
                        new Font(Font.SANS_SERIF, Font.BOLD, 16)
                )
        );

        globalBox.add(credentialsBox);
        globalBox.add(createRigidArea(0, 10));
        globalBox.add(buttonField);

        // Center everything on the x-axis
        Box horizontalBox = Box.createHorizontalBox();
        horizontalBox.add(Box.createHorizontalGlue());
        horizontalBox.add(globalBox);
        horizontalBox.add(Box.createHorizontalGlue());

        // Center everything on the y-axis
        Box verticalBox = Box.createVerticalBox();
        verticalBox.add(Box.createVerticalGlue());
        verticalBox.add(horizontalBox);
        verticalBox.add(Box.createVerticalGlue());

        add(verticalBox);

        buttonField.addActionListener(actionEvent -> {
            // TODO: Connect to MongoDB
        });
    }

    private static Component createRigidArea(int width, int height) {
        return Box.createRigidArea(new Dimension(width, height));
    }

    private static Box stickHorizontally(Component... components) {
        Box horizontalBox = Box.createHorizontalBox();
        for (Component component : components) {
            horizontalBox.add(component);
        }

        return horizontalBox;
    }

    private static JFormattedTextField.AbstractFormatterFactory createIntegerFormatter() {
        return new JFormattedTextField.AbstractFormatterFactory() {
            @Override
            public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField jFormattedTextField) {
                NumberFormat integerFormat = NumberFormat.getIntegerInstance();
                integerFormat.setGroupingUsed(false);
                integerFormat.setMinimumIntegerDigits(0);
                integerFormat.setMinimumIntegerDigits(0);
                integerFormat.setMaximumIntegerDigits(65535);

                NumberFormatter numberFormatter = new NumberFormatter(integerFormat);
                numberFormatter.setValueClass(Integer.class);
                numberFormatter.setAllowsInvalid(false);

                return numberFormatter;
            }
        };
    }
}
