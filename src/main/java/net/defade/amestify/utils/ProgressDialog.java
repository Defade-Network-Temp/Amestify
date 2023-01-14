package net.defade.amestify.utils;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;

public class ProgressDialog {
    private final JDialog dialog;
    private final JLabel messageLabel;
    private final JProgressBar progressBar;

    public ProgressDialog(Frame frame, int width, int height) {
        this.dialog = new JDialog(frame);
        dialog.setLayout(new FlowLayout(FlowLayout.CENTER));
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setSize(width, height);
        dialog.setPreferredSize(new Dimension(width, height));
        dialog.setResizable(false);
        dialog.setVisible(true);

        Box dialogBox = Box.createVerticalBox();

        this.messageLabel = new JLabel();
        messageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        messageLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        dialogBox.add(messageLabel);

        this.progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
        dialogBox.add(progressBar);

        dialog.add(dialogBox);
    }

    public void setTitle(String title) {
        dialog.setTitle(title);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void setIndeterminateProgress(boolean indeterminate) {
        progressBar.setIndeterminate(indeterminate);
    }

    public void setValue(int value) {
        progressBar.setValue(value);
    }

    public int getValue() {
        return progressBar.getValue();
    }

    public void setMaximumValue(int maximumValue) {
        progressBar.setMaximum(maximumValue);
    }

    public void close() {
        dialog.dispose();
    }

    public void setBarText(String text) {
        if(text == null) {
            progressBar.setStringPainted(false);
        } else {
            progressBar.setStringPainted(true);
            progressBar.setString(text);
        }
    }
}
