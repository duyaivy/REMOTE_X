package client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Authentication extends JFrame implements ActionListener {
    private Socket cSocket = null;
    DataOutputStream passwordCheck = null;
    DataInputStream verification = null;
    String verify = "";
    JButton submitBtn;
    JPanel panel;
    JLabel label, passwordLabel;
    String width = "", height = "";
    JTextField passwordInput;

    Authentication(Socket sSocket) {
        passwordLabel = new JLabel();
        passwordLabel.setText("Password");
        passwordInput = new JTextField(15);
        this.cSocket = sSocket;
        label = new JLabel();
        label.setText("");
        this.setLayout(new BorderLayout());
        submitBtn = new JButton("Submit");
        panel = new JPanel(new GridLayout(2, 1));
        panel.add(passwordLabel);
        panel.add(passwordInput);
        panel.add(label);
        panel.add(submitBtn);
        add(panel, BorderLayout.CENTER);
        submitBtn.addActionListener(this);
        setTitle("Login form");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String passwordVal = passwordInput.getText();
        try {
            passwordCheck = new DataOutputStream(cSocket.getOutputStream());
            verification = new DataInputStream(cSocket.getInputStream());

            passwordCheck.writeUTF(passwordVal);
            verify = verification.readUTF();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (verify.equals("valid")) {
            try {
                width = verification.readUTF();
                height = verification.readUTF();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            CreateFrame newFrame = new CreateFrame(cSocket, width, height);
            dispose();
        } else {
            System.out.println("Please enter a valid password");
            JOptionPane.showMessageDialog(this, "Incorrect login or password", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

}
