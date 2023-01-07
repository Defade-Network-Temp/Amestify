package net.defade.amestify.ui.gui;

import net.defade.amestify.database.MongoConnector;
import net.defade.amestify.ui.AmestifyWindow;
import org.jdesktop.swingx.prompt.PromptSupport;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.NumberFormatter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.text.NumberFormat;
import java.util.concurrent.CompletableFuture;

public class MongoDbLoginGUI extends JPanel {
    private final JTextField hostField = new JTextField();
    private final JFormattedTextField portField = new JFormattedTextField();
    private final JTextField databaseField = new JTextField();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton buttonField = new JButton();

    public MongoDbLoginGUI(AmestifyWindow amestifyWindow, MongoConnector mongoConnector) {
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
            buttonField.setEnabled(false);

            JDialog connectingDialog = new JDialog((Frame) null, "Connecting...");
            connectingDialog.setLayout(new FlowLayout(FlowLayout.CENTER));
            connectingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            connectingDialog.setSize(300, 100);
            connectingDialog.setPreferredSize(new Dimension(300, 100));
            connectingDialog.setResizable(false);
            connectingDialog.setVisible(true);

            Box connectingBox = Box.createVerticalBox();

            JLabel connectingLabel = new JLabel("Connecting to MongoDB...");
            connectingLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            connectingBox.add(connectingLabel);
            connectingLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

            JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
            progressBar.setIndeterminate(true);
            connectingBox.add(progressBar);

            connectingDialog.add(connectingBox);

            CompletableFuture<Void> future = mongoConnector.connect(
                    hostField.getText().isBlank() ? "localhost" : hostField.getText(),
                    portField.getText().isBlank() ? 27017 : Integer.parseInt(portField.getText()),
                    usernameField.getText().isBlank() ? "Defade" : usernameField.getText(),
                    passwordField.getPassword(),
                    databaseField.getText().isBlank() ? "defade" : databaseField.getText()
            );

            future.whenComplete((unused, throwable) -> {
                if(throwable != null) {
                    connectingDialog.dispose();
                    JOptionPane.showMessageDialog(amestifyWindow, throwable.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    buttonField.setEnabled(true);
                } else {
                    connectingDialog.dispose();
                    amestifyWindow.setContentPane(new AmestifyGUI());
                    amestifyWindow.pack();
                }
            });
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
