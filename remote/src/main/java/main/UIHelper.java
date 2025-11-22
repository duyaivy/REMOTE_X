package main;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UIHelper {

    public static final Color COLOR_TITLE = new Color(0, 114, 188);
    public static final Color COLOR_HEADER_BG = new Color(240, 248, 255);
    public static final Color COLOR_BACKGROUND = new Color(245, 245, 250);
    public static final Color COLOR_BUTTON = new Color(66, 139, 202);
    public static final Color COLOR_BUTTON_HOVER = new Color(51, 122, 183);
    public static final Color COLOR_TEXT_DARK = new Color(51, 51, 51);
    public static final Color COLOR_BORDER = new Color(220, 220, 220);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_FIELD = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 12);

    public static Border createPanelBorder() {
        Border lineBorder = BorderFactory.createLineBorder(COLOR_BORDER, 1);
        Border emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);
        return BorderFactory.createCompoundBorder(lineBorder, emptyBorder);
    }

    public static void styleTextField(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        field.setPreferredSize(new Dimension(200, 32));
    }

    public static JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setForeground(Color.WHITE);
        btn.setBackground(COLOR_BUTTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(180, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                if (btn.isEnabled()) {
                    btn.setBackground(COLOR_BUTTON_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                if (btn.isEnabled()) {
                    btn.setBackground(COLOR_BUTTON);
                }
            }
        });

        return btn;
    }

    public static JMenu createStyledMenu(String text) {
        JMenu menu = new JMenu(text);
        menu.setFont(FONT_LABEL);
        menu.setForeground(Color.WHITE);
        menu.setOpaque(false);
        return menu;
    }

    public static Icon createAntennaIcon() {
        return new Icon() {
            public int getIconWidth() {
                return 24;
            }

            public int getIconHeight() {
                return 24;
            }

            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_TITLE);

                g2.setStroke(new BasicStroke(2));
                g2.drawLine(x + 12, y + 8, x + 12, y + 20);
                g2.drawArc(x + 2, y + 12, 8, 8, 0, 180);
                g2.drawArc(x + 14, y + 12, 8, 8, 0, 180);
                g2.fillOval(x + 10, y + 6, 4, 4);
                g2.dispose();
            }
        };
    }

    public static Icon createComputerIcon() {
        return new Icon() {
            public int getIconWidth() {
                return 24;
            }

            public int getIconHeight() {
                return 24;
            }

            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_TITLE);

                g2.drawRect(x + 3, y + 4, 18, 12);
                g2.drawRect(x + 4, y + 5, 16, 10);

                g2.drawLine(x + 12, y + 16, x + 12, y + 19);
                g2.drawLine(x + 8, y + 19, x + 16, y + 19);

                g2.dispose();
            }
        };
    }

    public static JPanel createHeaderPanel(String title, Icon icon) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        header.setBackground(COLOR_HEADER_BG);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));

        JLabel iconLabel = new JLabel(icon);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(COLOR_TITLE);

        header.add(iconLabel);
        header.add(titleLabel);
        return header;
    }

    public static JPanel createStyledPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(createPanelBorder());
        panel.setBackground(Color.WHITE);
        return panel;
    }

    public static JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statusBar.setBackground(COLOR_BACKGROUND);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER));

        JLabel statusIcon = new JLabel("●");
        statusIcon.setFont(new Font("Dialog", Font.PLAIN, 14));
        statusIcon.setForeground(new Color(76, 175, 80)); // Màu xanh lá

        JLabel statusLabel = new JLabel("Sẵn sàng kết nối");
        statusLabel.setFont(FONT_LABEL);
        statusLabel.setForeground(COLOR_TEXT_DARK);

        statusBar.add(statusIcon);
        statusBar.add(statusLabel);

        return statusBar;
    }

    public static JMenuBar createStyledMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(COLOR_TITLE);
        menuBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JMenu menuFile = createStyledMenu("Tệp tin");
        JMenu menuSettings = createStyledMenu("Thiết lập");
        JMenu menuHelp = createStyledMenu("Trợ giúp");

        menuBar.add(menuFile);
        menuBar.add(menuSettings);
        menuBar.add(menuHelp);

        return menuBar;
    }
}