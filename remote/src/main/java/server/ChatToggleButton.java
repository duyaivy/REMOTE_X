package server;

import common.ChatWindow; // Import lớp ChatWindow
import javax.swing.*;
import java.awt.*;

public class ChatToggleButton {

    private JWindow window; 
    private ChatWindow chatWindow; 
    public ChatToggleButton(ChatWindow chatWindow) {
        this.chatWindow = chatWindow;
        SwingUtilities.invokeLater(this::createButton);
    }

    private void createButton() {
        window = new JWindow(); 
        window.setAlwaysOnTop(true); 
        
 
        JButton chatButton = new JButton("Chat");
        chatButton.setToolTipText("Mở cửa sổ chat");
        chatButton.setMargin(new Insets(5, 15, 5, 15)); 
        chatButton.setFocusable(false);
        chatButton.addActionListener(e -> {
            if (chatWindow != null) {
                chatWindow.showWindow();
            }
        });

      
        window.add(chatButton);
        
        window.pack(); 
        
       
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int yPosition = (screenSize.height - window.getHeight()) / 2;
        window.setLocation(0, yPosition);
        
       
        window.setVisible(true);
    }
}