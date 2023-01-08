package net.defade.amestify.ui.gui;

import org.jdesktop.swingx.prompt.PromptSupport;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Dimension;
import java.nio.file.Path;

public class ConverterGUI extends JPanel {
    private final JButton anvilFolderPathSelector = new JButton();
    private final JTextField fileNameField = new JTextField();
    private final JTextField fileIdField = new JTextField();
    private final JTextField miniGameNameField = new JTextField();
    private final JButton confirmButton = new JButton();

    public ConverterGUI() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        Box globalBox = Box.createVerticalBox();

        anvilFolderPathSelector.setText("Import Anvil File...");
        anvilFolderPathSelector.setPreferredSize(new Dimension(200, 48));
        anvilFolderPathSelector.setMaximumSize(new Dimension(200, 48));
        anvilFolderPathSelector.setAlignmentX(JTextField.CENTER_ALIGNMENT);

        PromptSupport.init("File Name", null, null, fileNameField);
        fileNameField.setPreferredSize(new Dimension(200, 48));
        fileNameField.setMaximumSize(new Dimension(200, 48));
        fileNameField.setAlignmentX(JTextField.CENTER_ALIGNMENT);

        PromptSupport.init("ID", null, null, fileIdField);
        fileIdField.setPreferredSize(new Dimension(200, 48));
        fileIdField.setMaximumSize(new Dimension(200, 48));
        fileIdField.setAlignmentX(JTextField.CENTER_ALIGNMENT);

        PromptSupport.init("Mini-game Name", null, null, miniGameNameField);
        miniGameNameField.setPreferredSize(new Dimension(200, 48));
        miniGameNameField.setMaximumSize(new Dimension(200, 48));
        miniGameNameField.setAlignmentX(JTextField.CENTER_ALIGNMENT);

        PromptSupport.init("Mini-game Name", null, null, miniGameNameField);
        miniGameNameField.setPreferredSize(new Dimension(200, 48));
        miniGameNameField.setMaximumSize(new Dimension(200, 48));
        miniGameNameField.setAlignmentX(JTextField.CENTER_ALIGNMENT);

        confirmButton.setText("Confirm");
        confirmButton.setPreferredSize(new Dimension(100, 32));
        confirmButton.setMaximumSize(new Dimension(100, 32));
        confirmButton.setAlignmentX(JTextField.LEFT_ALIGNMENT);

        globalBox.add(anvilFolderPathSelector);
        globalBox.add(createRigidArea(0, 4));
        globalBox.add(fileNameField);
        globalBox.add(createRigidArea(0, 4));
        globalBox.add(fileIdField);
        globalBox.add(createRigidArea(0, 4));
        globalBox.add(miniGameNameField);
        globalBox.add(createRigidArea(0, 4));
        globalBox.add(confirmButton);

        // Center on the y-axis
        Box horizontalBox = Box.createHorizontalBox();
        horizontalBox.add(Box.createHorizontalGlue());
        horizontalBox.add(globalBox);
        horizontalBox.add(Box.createHorizontalGlue());

        add(horizontalBox);

        anvilFolderPathSelector.addActionListener(actionEvent -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setFileHidingEnabled(false);

            if(fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                anvilFolderPathSelector.setText(fileChooser.getSelectedFile().getName());
            } else {
                anvilFolderPathSelector.setText("Import Anvil Folder...");
            }
        });

        confirmButton.addActionListener(actionEvent -> {
            if(anvilFolderPathSelector.getText().equals("Import Anvil Folder...")) {
                JOptionPane.showMessageDialog(this, "Please select an Anvil file first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } else if(fileNameField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a file name.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } else if(fileIdField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a file ID.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } else if(miniGameNameField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a mini-game name.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Path anvilFolder = Path.of(anvilFolderPathSelector.getText());
            String fileName = fileNameField.getText();
            String fileId = fileIdField.getText();
            String miniGameName = miniGameNameField.getText();

            // TODO: Launch converter
        });
    }

    private static Component createRigidArea(int width, int height) {
        return Box.createRigidArea(new Dimension(width, height));
    }
}
