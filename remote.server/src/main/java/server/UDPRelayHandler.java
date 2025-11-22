package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Map;

/**
 * Handler cho UDP relay - Forward screen data packets
 * Chạy trong một thread riêng, lắng nghe tất cả UDP packets
 */
public class UDPRelayHandler extends Thread {
    private final DatagramSocket socket;
    private final Map<String, Session> activeSessions;

    public UDPRelayHandler(DatagramSocket socket, Map<String, Session> activeSessions) {
        this.socket = socket;
        this.activeSessions = activeSessions;
        this.setName("UDP-Relay-Handler");
    }

    @Override
    public void run() {
        System.out.println("[UDPRelay] ═══════════════════════════════════");
        System.out.println("[UDPRelay] Started on port " + socket.getLocalPort());
        System.out.println("[UDPRelay] ═══════════════════════════════════");

        byte[] buffer = new byte[65536]; // Max UDP packet size

        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Parse packet
                byte[] data = Arrays.copyOf(buffer, packet.getLength());
                ScreenPacket.PacketInfo info = ScreenPacket.parsePacket(data);

                // Handle packet based on type
                handlePacket(info, packet, data);

            } catch (Exception e) {
                System.err.println("[UDPRelay] Error: " + e.getMessage());
            }
        }
    }

    /**
     * Xử lý packet dựa vào type
     */
    private void handlePacket(ScreenPacket.PacketInfo info,
            DatagramPacket originalPacket,
            byte[] data) throws Exception {

        InetAddress senderAddr = originalPacket.getAddress();
        int senderPort = originalPacket.getPort();

        switch (info.type) {
            case ScreenPacket.TYPE_REGISTER:
                handleRegister(info, senderAddr, senderPort);
                break;

            case ScreenPacket.TYPE_REQUEST:
                handleRequest(info, senderAddr, senderPort);
                break;

            case ScreenPacket.TYPE_FULL_FRAME:
            case ScreenPacket.TYPE_DELTA_FRAME:
                handleFrameData(info, data);
                break;

            case ScreenPacket.TYPE_ACK:
                handleAck(info, data);
                break;

            default:
                System.err.println("[UDPRelay] Unknown packet type: " + info.type);
        }
    }

    /**
     * Client đăng ký địa chỉ UDP với relay
     */
    private void handleRegister(ScreenPacket.PacketInfo info,
            InetAddress addr, int port) throws Exception {

        Session session = findSessionByClientId(info.clientId);

        if (session == null) {
            System.err.println("[UDPRelay] ✗ Session not found for clientId: " + info.clientId);
            return;
        }

        // Xác định đây là sharer hay viewer
        // Logic: client đăng ký đầu tiên là sharer, thứ hai là viewer
        if (session.getSharerUDPInfo() == null) {
            session.registerSharerUDP(addr, port, info.clientId);
            System.out.println("[UDPRelay] ✓ Registered SHARER: " + session.getUsername() +
                    " from " + addr + ":" + port);
        } else if (session.getViewerUDPInfo() == null) {
            session.registerViewerUDP(addr, port, info.clientId);
            System.out.println("[UDPRelay] ✓ Registered VIEWER: " + session.getUsername() +
                    " from " + addr + ":" + port);
        } else {
            System.err.println("[UDPRelay] ⚠ Both sharer and viewer already registered");
        }

        // Gửi ACK về client
        byte[] ack = "OK".getBytes();
        DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, addr, port);
        socket.send(ackPacket);
    }

    /**
     * Viewer yêu cầu frame mới
     */
    private void handleRequest(ScreenPacket.PacketInfo info,
            InetAddress viewerAddr, int viewerPort) throws Exception {

        Session session = findSessionByClientId(info.clientId);
        if (session == null) {
            System.err.println("[UDPRelay] ✗ Session not found for REQUEST");
            return;
        }

        Session.UDPClientInfo sharerInfo = session.getSharerUDPInfo();
        if (sharerInfo == null) {
            System.err.println("[UDPRelay] ✗ Sharer not registered yet");
            return;
        }

        // Forward REQUEST to sharer
        byte[] requestPacket = ScreenPacket.createRequestPacket(info.clientId);
        DatagramPacket forward = new DatagramPacket(
                requestPacket, requestPacket.length,
                sharerInfo.address, sharerInfo.port);
        socket.send(forward);

        // Log mỗi 10 requests
        if (info.clientId % 10 == 0) {
            System.out.println("[UDPRelay] → Forwarded REQUEST to sharer");
        }
    }

    /**
     * Sharer gửi FRAME data (FULL hoặc DELTA)
     */
    private void handleFrameData(ScreenPacket.PacketInfo info, byte[] data)
            throws Exception {

        Session session = findSessionByClientId(info.clientId);
        if (session == null) {
            System.err.println("[UDPRelay] ✗ Session not found for FRAME");
            return;
        }

        Session.UDPClientInfo viewerInfo = session.getViewerUDPInfo();
        if (viewerInfo == null) {
            System.err.println("[UDPRelay] ✗ Viewer not registered yet");
            return;
        }

        // Forward FRAME to viewer
        DatagramPacket forward = new DatagramPacket(
                data, data.length,
                viewerInfo.address, viewerInfo.port);
        socket.send(forward);

        // Log mỗi 30 frames
        if (info.sequence % 30 == 0) {
            String type = (info.type == ScreenPacket.TYPE_FULL_FRAME) ? "FULL" : "DELTA";
            System.out.println("[UDPRelay] → Forwarded " + type + " frame #" +
                    info.sequence + " (" + data.length / 1024 + "KB) to viewer");
        }
    }

    /**
     * Viewer gửi ACK về sharer
     */
    private void handleAck(ScreenPacket.PacketInfo info, byte[] data)
            throws Exception {

        Session session = findSessionByClientId(info.clientId);
        if (session == null)
            return;

        Session.UDPClientInfo sharerInfo = session.getSharerUDPInfo();
        if (sharerInfo == null)
            return;

        // Forward ACK to sharer
        DatagramPacket forward = new DatagramPacket(
                data, data.length,
                sharerInfo.address, sharerInfo.port);
        socket.send(forward);
    }

    /**
     * Tìm session dựa vào clientId
     * clientId = username.hashCode()
     */
    private Session findSessionByClientId(int clientId) {
        for (Session session : activeSessions.values()) {
            if (session.getUsername().hashCode() == clientId) {
                return session;
            }
        }
        return null;
    }
}