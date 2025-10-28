package common; // Hoặc package 'shared' của bạn

import javax.swing.*;
import javax.swing.text.*; // <-- Import thêm để hỗ trợ style
import java.awt.*;
import java.awt.event.FocusEvent; // <-- Import thêm cho placeholder
import java.awt.event.FocusListener; // <-- Import thêm cho placeholder
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatWindow {

    // --- Biến UI ---
    private JFrame frame;
    // THAY ĐỔI: Đổi JTextArea thành JTextPane
    private JTextPane chatPane; 
    private JTextField messageField;
    private SimpleDateFormat timeFormatter;
    private String placeholderText = "Nhấn Enter để gửi...";

    // --- Biến Mạng ---
    private final DataOutputStream out;
    private final DataInputStream in;
    private final String partnerName;

    // --- Biến Style (để định dạng màu) ---
    private Style styleMe;
    private Style stylePartner;
    private Style styleTime;

    public ChatWindow(Socket socket, String partnerName) {
        this.partnerName = partnerName;
        DataOutputStream tempOut = null;
        DataInputStream tempIn = null;
        
        try {
            tempIn = new DataInputStream(socket.getInputStream());
            tempOut = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        this.in = tempIn;
        this.out = tempOut;
        this.timeFormatter = new SimpleDateFormat("HH:mm"); // Format: 18:40

        startReceivingMessages();
    }

    public void showWindow() {
        if (frame == null) {
            frame = new JFrame("Chat voi " + partnerName);
            frame.setSize(350, 450); // Kích thước giống UltraViewer
            frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); 
            frame.setLayout(new BorderLayout(5, 5)); // Thêm khoảng cách

            // --- NÂNG CẤP LÊN JTextPane ---
            chatPane = new JTextPane();
            chatPane.setEditable(false);
            chatPane.setMargin(new Insets(5, 5, 5, 5)); // Thêm padding
            
            // Định nghĩa các style
            styleTime = chatPane.addStyle("Time", null);
            StyleConstants.setForeground(styleTime, Color.GRAY);
            StyleConstants.setFontSize(styleTime, 11);

            styleMe = chatPane.addStyle("Me", null);
            StyleConstants.setForeground(styleMe, Color.BLACK);

            stylePartner = chatPane.addStyle("Partner", null);
            StyleConstants.setForeground(stylePartner, new Color(0, 0, 150)); // Màu xanh đậm
            StyleConstants.setBold(stylePartner, true);
            
            JScrollPane scrollPane = new JScrollPane(chatPane);
            scrollPane.setBorder(BorderFactory.createTitledBorder("Lịch sử chat")); // Thêm tiêu đề
            frame.add(scrollPane, BorderLayout.CENTER);

            // --- NÂNG CẤP JTextField (với placeholder) ---
            messageField = new JTextField();
            setPlaceholder(); // <-- Gọi hàm đặt placeholder

            // Thêm sự kiện Focus để bật/tắt placeholder
            messageField.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (messageField.getText().equals(placeholderText)) {
                        messageField.setText("");
                        messageField.setForeground(Color.BLACK);
                    }
                }
                @Override
                public void focusLost(FocusEvent e) {
                    if (messageField.getText().isEmpty()) {
                        setPlaceholder();
                    }
                }
            });
            
            messageField.addActionListener(e -> sendMessage());
            
            // Gói messageField trong 1 JPanel để thêm border
            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5)); // Padding
            bottomPanel.add(messageField, BorderLayout.CENTER);
            
            // Thêm nút "Gửi" (giống UltraViewer có nút Send)
            JButton sendButton = new JButton("Gửi");
            sendButton.addActionListener(e -> sendMessage());
            bottomPanel.add(sendButton, BorderLayout.EAST);
            
            frame.add(bottomPanel, BorderLayout.SOUTH);
        }

        frame.setVisible(true);
        frame.toFront();
    }

    // Hàm đặt placeholder
    private void setPlaceholder() {
        messageField.setText(placeholderText);
        messageField.setForeground(Color.GRAY);
    }

    // Hàm gửi tin nhắn (đã nâng cấp)
    private void sendMessage() {
        String message = messageField.getText().trim();
        
        // Không gửi nếu là placeholder hoặc rỗng
        if (message.isEmpty() || message.equals(placeholderText)) {
            return;
        }
        
        try {
            out.writeUTF(message);
            out.flush();
            
            // Format: Tôi (18:40): hi
            String time = timeFormatter.format(new Date());
            appendToPane("Tôi", " (" + time + "): " + message + "\n", styleMe, styleTime);

            messageField.setText("");
            
        } catch (IOException ex) {
            appendToPane("Lỗi", ": " + ex.getMessage() + "\n", stylePartner, styleTime);
        }
    }

    // Luồng nhận tin nhắn (đã nâng cấp)
    private void startReceivingMessages() {
        new Thread(() -> {
            try {
                while (true) {
                    String message = in.readUTF(); 
                    String time = timeFormatter.format(new Date());
                    
                    // Tự động bật cửa sổ nếu đang ẩn
                    if (frame == null || !frame.isVisible()) {
                        SwingUtilities.invokeLater(this::showWindow);
                    }
                    
                    // Format: DESKTOP-USJOR77 (18:40): chaof
                    SwingUtilities.invokeLater(() -> {
                        appendToPane(partnerName, " (" + time + "): " + message + "\n", stylePartner, styleTime);
                    });
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    appendToPane("---", " " + partnerName + " đã ngắt kết nối ---", stylePartner, styleTime);
                });
            }
        }).start();
    }

    /**
     * Hàm helper quan trọng nhất
     * Dùng để thêm text với nhiều style khác nhau vào JTextPane
     */
    private void appendToPane(String name, String message, Style nameStyle, Style timeStyle) {
        try {
            StyledDocument doc = chatPane.getStyledDocument();
            
            // Tách phần thời gian ra khỏi tin nhắn
            // Ví dụ: " (18:40): hi\n"
            int timeEndIndex = message.indexOf("): ");
            String timePart = message.substring(0, timeEndIndex + 1); // " (18:40)"
            String msgPart = message.substring(timeEndIndex + 1);    // ": hi\n"
            
            // 1. Thêm Tên (in đậm/màu)
            doc.insertString(doc.getLength(), name, nameStyle);
            // 2. Thêm Thời gian (màu xám)
            doc.insertString(doc.getLength(), timePart, timeStyle);
            // 3. Thêm Tin nhắn (style giống Tên)
            doc.insertString(doc.getLength(), msgPart, nameStyle);
            
            // Tự cuộn xuống dưới
            chatPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}