package net.defade.amestify.ui.gui;

import net.defade.amestify.loaders.AmethystSaver;
import net.defade.amestify.loaders.anvil.AnvilMap;
import net.defade.amestify.ui.AmestifyWindow;
import net.defade.amestify.utils.MongoFileUploader;
import net.defade.amestify.utils.ProgressDialog;
import net.defade.amestify.utils.Utils;
import net.defade.amestify.world.World;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ConverterGUI extends JPanel {
    private final MongoFileUploader mongoFileUploader;

    private final JButton anvilFolderPathSelector = new JButton();
    private final JTextField fileNameField = new JTextField();
    private final JTextField fileIdField = new JTextField();
    private final JTextField miniGameNameField = new JTextField();

    private Path anvilPath = null;

    public ConverterGUI(AmestifyWindow amestifyWindow) {
        this.mongoFileUploader = new MongoFileUploader(amestifyWindow.getMongoConnector());
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

        JButton confirmButton = new JButton();
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
                anvilPath = fileChooser.getSelectedFile().toPath();
            } else {
                anvilFolderPathSelector.setText("Import Anvil Folder...");
            }
        });

        confirmButton.addActionListener(actionEvent -> {
            if(anvilPath == null) {
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

            String fileName = fileNameField.getText();
            String fileId = fileIdField.getText();
            String miniGameName = miniGameNameField.getText();

            ProgressDialog progressDialog = new ProgressDialog(amestifyWindow, 400, 100);
            CompletableFuture<World> worldFuture = new AnvilMap(anvilPath, -64, 320).loadWorld(progressDialog);

            worldFuture.whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    JOptionPane.showMessageDialog(this, "An error occurred while converting the Anvil file.", "Error", JOptionPane.ERROR_MESSAGE);
                    throwable.printStackTrace();
                } else {
                    convertAnvilWorld(worldFuture.join(), fileName, fileId, miniGameName, progressDialog);
                }
            });
        });
    }

    private void convertAnvilWorld(World world, String fileName, String fileId, String miniGameName, ProgressDialog progressDialog) {
        try {
            Path amethystFile = Utils.createAmethystTempFile(fileName);
            AmethystSaver amethystSaver = new AmethystSaver(Files.newOutputStream(amethystFile), world);

            amethystSaver.save(progressDialog).whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    JOptionPane.showMessageDialog(this, "An error occurred while converting the Amethyst file.", "Error", JOptionPane.ERROR_MESSAGE);
                    throwable.printStackTrace();
                } else {
                    sendAmethystFileToMongoDB(amethystFile, fileName, fileId, miniGameName, progressDialog);
                }
            });
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "An error occurred while converting the Amethyst file.", "Error", JOptionPane.ERROR_MESSAGE);
            exception.printStackTrace();
        }
    }

    private void sendAmethystFileToMongoDB(Path amethystFile, String fileName, String fileId, String miniGameName, ProgressDialog progressDialog) {
        try {
            mongoFileUploader.sendFile(amethystFile, fileName, fileId, miniGameName, progressDialog);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "An error occurred while sending the Amethyst file.", "Error", JOptionPane.ERROR_MESSAGE);
            exception.printStackTrace();
        }
    }

    private static Component createRigidArea(int width, int height) {
        return Box.createRigidArea(new Dimension(width, height));
    }
}
