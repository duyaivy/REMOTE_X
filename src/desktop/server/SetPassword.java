package server;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class SetPassword extends JFrame implements ActionListener {
    static String port = "8080";
    JButton submitBtn;
    JPanel panel;
    JTextField setPasswordTextField, text2;
    String passwordValue, value2;
    JLabel label, setPasswordLabel, label2;

    SetPassword() {
        setPasswordLabel = new JLabel();
        setPasswordLabel.setText("Set Password :");
        setPasswordTextField = new JTextField(15);
        label = new JLabel();
        this.setLayout(new BorderLayout());
        submitBtn = new JButton("Submit");
        panel = new JPanel(new GridLayout(2, 1));
        panel.add(setPasswordLabel);
        panel.add(setPasswordTextField);
        panel.add(label);
        panel.add(submitBtn);
        add(panel, BorderLayout.CENTER);
        submitBtn.addActionListener(this);
        setTitle("Setting password for client");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        passwordValue = setPasswordTextField.getText();
        dispose();
        new InitConnection(Integer.parseInt(port), passwordValue);
    }

    public String getPasswordValue() {
        return passwordValue;
    }

    public static void main(String[] args) {
        SetPassword setPasswordFrame = new SetPassword();
        setPasswordFrame.setSize(300, 80);
        setPasswordFrame.setLocation(500, 300);
        setPasswordFrame.setVisible(true);
    }
}
