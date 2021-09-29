package wow.sniffer;

public class Packet {
    private final int opcode;
    private final int size;
    private final int timestamp;
    private final byte type;
    private final byte[] data;

    public Packet(int opcode, int size, int timestamp, byte type, byte[] data) {
        this.opcode = opcode;
        this.size = size;
        this.timestamp = timestamp;
        this.type = type;
        this.data = data;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (type == 0) {
            sb.append("CMSG: ");
        } else if (type == 1) {
            sb.append("SMSG: ");
        } else {
            sb.append("unknownType(").append(Integer.toHexString(type)).append(") ");
        }

        sb.append("opcode(").append(Integer.toHexString(opcode)).append(") ");
        sb.append("length: ").append(size).append(" ");
        sb.append("timestamp: ").append(timestamp).append("\n");
        if (size > 0) {
            sb.append("data: ").append(Utils.bytesToHex(data));
        }

        return sb.toString();
    }

    public int getOpcode() {
        return opcode;
    }

    public int getSize() {
        return size;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public byte getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }
}
