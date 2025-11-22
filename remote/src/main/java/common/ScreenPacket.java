package common;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class ScreenPacket {
    // Packet types
    public static final byte TYPE_DATA = 0x01;
    public static final byte TYPE_ACK = 0x02;
    public static final byte TYPE_REGISTER = 0x03;
    public static final byte TYPE_REQUEST = 0x04;

    // [Magic(2)][Type(1)][ClientID(4)][FrameID(4)][SeqNum(2)][TotalPackets(2)][Length(2)][Checksum(4)][Data]
    private static final short MAGIC = (short) 0xABCD;
    private static final int HEADER_SIZE = 21;

    /**
     * Tạo DATA packet
     */
    public static byte[] createDataPacket(int clientId, int frameId, int seqNum,
            int totalPackets, byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + data.length);

        buffer.putShort(MAGIC); // 2 bytes
        buffer.put(TYPE_DATA); // 1 byte
        buffer.putInt(clientId); // 4 bytes - ID của client nhận
        buffer.putInt(frameId); // 4 bytes
        buffer.putShort((short) seqNum); // 2 bytes
        buffer.putShort((short) totalPackets); // 2 bytes
        buffer.putShort((short) data.length); // 2 bytes

        // Checksum
        CRC32 crc = new CRC32();
        crc.update(data);
        buffer.putInt((int) crc.getValue()); // 4 bytes

        buffer.put(data);

        return buffer.array();
    }

    /**
     * Tạo ACK packet
     */
    public static byte[] createAckPacket(int clientId, int frameId, int seqNum) {
        ByteBuffer buffer = ByteBuffer.allocate(13);

        buffer.putShort(MAGIC); // 2 bytes
        buffer.put(TYPE_ACK); // 1 byte
        buffer.putInt(clientId); // 4 bytes
        buffer.putInt(frameId); // 4 bytes
        buffer.putShort((short) seqNum); // 2 bytes

        return buffer.array();
    }

    /**
     * Tạo REGISTER packet - Client đăng ký với Relay
     */
    public static byte[] createRegisterPacket(int clientId) {
        ByteBuffer buffer = ByteBuffer.allocate(7);

        buffer.putShort(MAGIC); // 2 bytes
        buffer.put(TYPE_REGISTER); // 1 byte
        buffer.putInt(clientId); // 4 bytes

        return buffer.array();
    }

    /**
     * Tạo REQUEST packet - Client yêu cầu frame mới
     */
    public static byte[] createRequestPacket(int clientId) {
        ByteBuffer buffer = ByteBuffer.allocate(7);

        buffer.putShort(MAGIC); // 2 bytes
        buffer.put(TYPE_REQUEST); // 1 byte
        buffer.putInt(clientId); // 4 bytes

        return buffer.array();
    }

    /**
     * Parse packet
     */
    public static PacketInfo parsePacket(byte[] packet) throws Exception {
        if (packet.length < 7) {
            throw new Exception("Packet too short");
        }

        ByteBuffer buffer = ByteBuffer.wrap(packet);

        short magic = buffer.getShort();
        if (magic != MAGIC) {
            throw new Exception("Invalid packet magic: " + Integer.toHexString(magic & 0xFFFF));
        }

        byte type = buffer.get();
        int clientId = buffer.getInt();

        PacketInfo info = new PacketInfo();
        info.type = type;
        info.clientId = clientId;

        if (type == TYPE_DATA) {
            if (packet.length < HEADER_SIZE) {
                throw new Exception("Data packet too short");
            }

            info.frameId = buffer.getInt();
            info.seqNum = buffer.getShort() & 0xFFFF;
            info.totalPackets = buffer.getShort() & 0xFFFF;
            int length = buffer.getShort() & 0xFFFF;
            int checksum = buffer.getInt();

            info.data = new byte[length];
            buffer.get(info.data);

            // Verify checksum
            CRC32 crc = new CRC32();
            crc.update(info.data);
            if ((int) crc.getValue() != checksum) {
                throw new Exception("Checksum mismatch");
            }

        } else if (type == TYPE_ACK) {
            info.frameId = buffer.getInt();
            info.seqNum = buffer.getShort() & 0xFFFF;
        }

        return info;
    }

    public static class PacketInfo {
        public byte type;
        public int clientId;
        public int frameId;
        public int seqNum;
        public int totalPackets;
        public byte[] data;

        @Override
        public String toString() {
            return String.format("Packet{type=%d, clientId=%d, frameId=%d, seqNum=%d, totalPackets=%d, dataLen=%d}",
                    type, clientId, frameId, seqNum, totalPackets, data != null ? data.length : 0);
        }
    }
}