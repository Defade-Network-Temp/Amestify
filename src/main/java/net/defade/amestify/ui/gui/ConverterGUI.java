package net.defade.amestify.ui.gui;

import net.defade.amestify.database.MongoConnector;
import net.defade.amestify.map.AnvilConverter;
import org.jdesktop.swingx.prompt.PromptSupport;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ConverterGUI extends JPanel {
    private final JButton anvilFolderPathSelector = new JButton();
    private final JTextField fileNameField = new JTextField();
    private final JTextField fileIdField = new JTextField();
    private final JTextField miniGameNameField = new JTextField();
    private final JButton confirmButton = new JButton();

    public ConverterGUI(MongoConnector mongoConnector) {
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

            AnvilConverter anvilConverter = new AnvilConverter(mongoConnector, anvilFolder, fileName, fileId, miniGameName);
            CompletableFuture<Void> future = anvilConverter.convert();

            // Create the progress dialog
            JDialog progressDialog = new JDialog((Frame) null, "Converting...");
            progressDialog.setLayout(new FlowLayout(FlowLayout.CENTER));
            progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            progressDialog.setSize(400, 100);
            progressDialog.setPreferredSize(new Dimension(400, 100));
            progressDialog.setResizable(false);
            progressDialog.setVisible(true);

            Box convertingBox = Box.createVerticalBox();

            JLabel convertingLabel = new JLabel("Converting the Anvil world to the Amethyst format...");
            convertingLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            convertingBox.add(convertingLabel);
            convertingLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

            JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
            convertingBox.add(progressBar);

            progressDialog.add(convertingBox);
            Timer timer = new Timer(100, null);
            timer.addActionListener(timerEvent -> {
                if (future.isDone()) {
                    timer.stop();
                    progressDialog.dispose();
                } else {
                    progressBar.setValue((int) (anvilConverter.getProgress()));
                }
            });
        });
    }

    private static Component createRigidArea(int width, int height) {
        return Box.createRigidArea(new Dimension(width, height));
    }
}
