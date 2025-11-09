package server;

import common.ChatWindow; // Import lớp ChatWindow
import javax.swing.*;
import java.awt.*;

/**
 * Lớp này tạo ra một cửa sổ nhỏ (JWindow) không viền, luôn nổi lên trên.
 * Nó chứa một nút "Chat" để kích hoạt cửa sổ chat chính.
 */
public class ChatToggleButton {

    private JWindow window; // Dùng JWindow để nó không có viền
    private ChatWindow chatWindow; // Tham chiếu đến cửa sổ chat logic

    public ChatToggleButton(ChatWindow chatWindow) {
        this.chatWindow = chatWindow;

        // Tạo UI trên Event Dispatch Thread
        SwingUtilities.invokeLater(this::createButton);
    }

    private void createButton() {
        window = new JWindow(); // JWindow là cửa sổ "trần"
        window.setAlwaysOnTop(true); // Luôn luôn nổi lên trên

        // Tạo nút bấm
        JButton chatButton = new JButton("Chat");
        chatButton.setToolTipText("Mở cửa sổ chat");
        chatButton.setMargin(new Insets(5, 15, 5, 15)); // Cho nút to ra một chút
        chatButton.setFocusable(false); // Không cướp focus

        // Thêm hành động: Khi bấm, gọi hàm showWindow() của ChatWindow
        chatButton.addActionListener(e -> {
            if (chatWindow != null) {
                chatWindow.showWindow();
            }
        });

        // Thêm nút vào cửa sổ
        window.add(chatButton);

        // Tự động điều chỉnh kích thước cửa sổ cho vừa với nút
        window.pack();

        // Đặt vị trí (ví dụ: ở giữa, bên trái màn hình)
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int yPosition = (screenSize.height - window.getHeight()) / 2; // Giữa chiều cao
        window.setLocation(0, yPosition); // Sát lề trái

        // Hiển thị nút bấm
        window.setVisible(true);
    }
}