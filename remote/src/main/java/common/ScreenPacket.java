package common;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * Utility class để tạo và parse UDP packets cho screen sharing
 * Dùng chung cho Server, Relay, và Client
 */
public class ScreenPacket {
    // Packet types
    public static final byte TYPE_FULL_FRAME = 0x01; // Full frame (toàn bộ màn hình)
    public static final byte TYPE_DELTA_FRAME = 0x02; // Delta frame (vùng thay đổi)
    public static final byte TYPE_ACK = 0x03; // Acknowledge
    public static final byte TYPE_REGISTER = 0x04; // Đăng ký với relay
    public static final byte TYPE_REQUEST = 0x05; // Yêu cầu frame mới

    private static final short MAGIC = (short) 0xABCD; // Magic number để verify packet

    /**
     * Tạo FULL FRAME packet
     * Format:
     * [Magic(2)][Type(1)][ClientID(4)][Sequence(4)][Width(4)][Height(4)][DataLen(4)][CRC32(4)][JPG
     * Data]
     */
    public static byte[] createFullFramePacket(int clientId, int sequence,
            int width, int height, byte[] jpgData) {
        ByteBuffer buffer = ByteBuffer.allocate(27 + jpgData.length);

        buffer.putShort(MAGIC); // 2 bytes
        buffer.put(TYPE_FULL_FRAME); // 1 byte
        buffer.putInt(clientId); // 4 bytes
        buffer.putInt(sequence); // 4 bytes
        buffer.putInt(width); // 4 bytes
        buffer.putInt(height); // 4 bytes
        buffer.putInt(jpgData.length); // 4 bytes

        CRC32 crc = new CRC32();
        crc.update(jpgData);
        buffer.putInt((int) crc.getValue()); // 4 bytes

        buffer.put(jpgData);

        return buffer.array();
    }

    /**
     * Tạo DELTA FRAME packet
     * Format: [Magic(2)][Type(1)][ClientID(4)][Sequence(4)][Width(4)][Height(4)]
     * [X(4)][Y(4)][DeltaW(4)][DeltaH(4)][DataLen(4)][CRC32(4)][JPG Data]
     */
    public static byte[] createDeltaFramePacket(int clientId, int sequence,
            int screenWidth, int screenHeight,
            int x, int y, int deltaWidth, int deltaHeight,
            byte[] jpgData) {
        ByteBuffer buffer = ByteBuffer.allocate(43 + jpgData.length);

        buffer.putShort(MAGIC); // 2 bytes
        buffer.put(TYPE_DELTA_FRAME); // 1 byte
        buffer.putInt(clientId); // 4 bytes
        buffer.putInt(sequence); // 4 bytes
        buffer.putInt(screenWidth); // 4 bytes
        buffer.putInt(screenHeight); // 4 bytes
        buffer.putInt(x); // 4 bytes
        buffer.putInt(y); // 4 bytes
        buffer.putInt(deltaWidth); // 4 bytes
        buffer.putInt(deltaHeight); // 4 bytes
        buffer.putInt(jpgData.length); // 4 bytes

        CRC32 crc = new CRC32();
        crc.update(jpgData);
        buffer.putInt((int) crc.getValue()); // 4 bytes

        buffer.put(jpgData);

        return buffer.array();
    }

    /**
     * Tạo ACK packet
     */
    public static byte[] createAckPacket(int clientId, int sequence) {
        ByteBuffer buffer = ByteBuffer.allocate(11);
        buffer.putShort(MAGIC);
        buffer.put(TYPE_ACK);
        buffer.putInt(clientId);
        buffer.putInt(sequence);
        return buffer.array();
    }

    /**
     * Tạo REGISTER packet - Client đăng ký địa chỉ UDP với relay
     */
    public static byte[] createRegisterPacket(int clientId) {
        ByteBuffer buffer = ByteBuffer.allocate(7);
        buffer.putShort(MAGIC);
        buffer.put(TYPE_REGISTER);
        buffer.putInt(clientId);
        return buffer.array();
    }

    /**
     * Tạo REQUEST packet - Client yêu cầu frame mới
     */
    public static byte[] createRequestPacket(int clientId) {
        ByteBuffer buffer = ByteBuffer.allocate(7);
        buffer.putShort(MAGIC);
        buffer.put(TYPE_REQUEST);
        buffer.putInt(clientId);
        return buffer.array();
    }

    /**
     * Parse packet - Giải mã packet nhận được
     */
    public static PacketInfo parsePacket(byte[] data) throws Exception {
        if (data.length < 7) {
            throw new Exception("Packet quá ngắn: " + data.length + " bytes");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);

        short magic = buffer.getShort();
        if (magic != MAGIC) {
            throw new Exception("Invalid magic: 0x" + Integer.toHexString(magic & 0xFFFF));
        }

        PacketInfo info = new PacketInfo();
        info.type = buffer.get();
        info.clientId = buffer.getInt();

        switch (info.type) {
            case TYPE_FULL_FRAME:
                if (data.length < 27) {
                    throw new Exception("Full frame packet quá ngắn");
                }
                info.sequence = buffer.getInt();
                info.width = buffer.getInt();
                info.height = buffer.getInt();
                int fullLen = buffer.getInt();
                int fullCrc = buffer.getInt();

                info.jpgData = new byte[fullLen];
                buffer.get(info.jpgData);

                // Verify CRC
                CRC32 crc1 = new CRC32();
                crc1.update(info.jpgData);
                if ((int) crc1.getValue() != fullCrc) {
                    throw new Exception("CRC mismatch - data corrupted");
                }
                break;

            case TYPE_DELTA_FRAME:
                if (data.length < 43) {
                    throw new Exception("Delta frame packet quá ngắn");
                }
                info.sequence = buffer.getInt();
                info.width = buffer.getInt();
                info.height = buffer.getInt();
                info.deltaX = buffer.getInt();
                info.deltaY = buffer.getInt();
                info.deltaWidth = buffer.getInt();
                info.deltaHeight = buffer.getInt();
                int deltaLen = buffer.getInt();
                int deltaCrc = buffer.getInt();

                info.jpgData = new byte[deltaLen];
                buffer.get(info.jpgData);

                // Verify CRC
                CRC32 crc2 = new CRC32();
                crc2.update(info.jpgData);
                if ((int) crc2.getValue() != deltaCrc) {
                    throw new Exception("CRC mismatch - data corrupted");
                }
                break;

            case TYPE_ACK:
                info.sequence = buffer.getInt();
                break;

            case TYPE_REGISTER:
            case TYPE_REQUEST:
                // Chỉ có clientId, không có data thêm
                break;

            default:
                throw new Exception("Unknown packet type: " + info.type);
        }

        return info;
    }

    /**
     * Data Transfer Object - Chứa thông tin đã parse
     */
    public static class PacketInfo {
        public byte type;
        public int clientId;
        public int sequence;
        public int width;
        public int height;
        public int deltaX;
        public int deltaY;
        public int deltaWidth;
        public int deltaHeight;
        public byte[] jpgData;

        @Override
        public String toString() {
            String typeStr = "";
            switch (type) {
                case TYPE_FULL_FRAME:
                    typeStr = "FULL";
                    break;
                case TYPE_DELTA_FRAME:
                    typeStr = "DELTA";
                    break;
                case TYPE_ACK:
                    typeStr = "ACK";
                    break;
                case TYPE_REGISTER:
                    typeStr = "REG";
                    break;
                case TYPE_REQUEST:
                    typeStr = "REQ";
                    break;
                default:
                    typeStr = "UNKNOWN";
                    break;
            }

            if (type == TYPE_FULL_FRAME || type == TYPE_DELTA_FRAME) {
                return String.format("Packet{type=%s, client=%d, seq=%d, screen=%dx%d, delta=(%d,%d,%dx%d), data=%dKB}",
                        typeStr, clientId, sequence, width, height,
                        deltaX, deltaY, deltaWidth, deltaHeight,
                        jpgData != null ? jpgData.length / 1024 : 0);
            } else {
                return String.format("Packet{type=%s, client=%d, seq=%d}",
                        typeStr, clientId, sequence);
            }
        }
    }
}