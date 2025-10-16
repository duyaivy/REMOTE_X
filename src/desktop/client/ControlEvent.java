package client;

import java.net.Socket;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.DataOutputStream;
import javax.swing.JPanel;

public class ControlEvent implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private int h, w;
    private DataOutputStream dos;
    // giam so lan event gui di
    private long lastMouseMoveTime = 0;
    private long lastMouseDragTime = 0;
    private static final long MOUSE_MOVE_DELAY = 50;
    private static final long MOUSE_DRAG_DELAY = 50;

    public ControlEvent(Socket socket, JPanel panel) {
        h = panel.getHeight();
        w = panel.getWidth();

        panel.addKeyListener(this);
        panel.addMouseListener(this);
        panel.addMouseMotionListener(this);
        panel.addMouseWheelListener(this);
        panel.setFocusable(true);
        try {
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        try {
            String control = Commands.PRESS_MOUSE.getAbbrev() + "," + e.getButton();
            dos.writeUTF(control);
            dos.flush();
        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        try {
            String control = Commands.RELEASE_MOUSE.getAbbrev() + "," + e.getButton();
            dos.writeUTF(control);
            dos.flush();
        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Throttling
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMouseDragTime < MOUSE_DRAG_DELAY) {
            return;
        }
        lastMouseDragTime = currentTime;

        int x = e.getX();
        int y = e.getY();
        try {

            String control = Commands.MOUSE_DRAGGED.getAbbrev() + ","
                    + e.getButton() + "," + scaleX(x) + "," + scaleY(y);
            dos.writeUTF(control);
            dos.flush();
        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Throttling: chỉ gửi sau mỗi MOUSE_MOVE_DELAY ms
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMouseMoveTime < MOUSE_MOVE_DELAY) {
            return; // Bỏ qua event này để tránh quá tải socket
        }
        lastMouseMoveTime = currentTime;

        int x = e.getX();
        int y = e.getY();
        try {

            String control = Commands.MOVE_MOUSE.getAbbrev() + "," + e.getButton() + "," + scaleX(x) + "," + scaleY(y);
            dos.writeUTF(control);
            dos.flush();
        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        try {
            String control = Commands.MOUSE_WHEEL_MOVED.getAbbrev() + "," + e.getWheelRotation();
            dos.writeUTF(control);
            dos.flush();
        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }

    private double scaleX(int x) {
        return (double) x / w;
    }

    private double scaleY(int y) {
        return (double) y / h;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            String control = Commands.PRESS_KEY.getAbbrev() + "," + e.getKeyCode();
            dos.writeUTF(control);
            dos.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        try {
            String control = Commands.RELEASE_KEY.getAbbrev() + "," + e.getKeyCode();
            dos.writeUTF(control);
            dos.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}