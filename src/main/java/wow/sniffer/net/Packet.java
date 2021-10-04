package wow.sniffer.net;

import wow.sniffer.Utils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Date;

public class Packet {
    private final int opcode;
    private final int size;
    private final Date timestamp;
    private final byte type;
    private final byte[] data;

    public Packet(int opcode, int size, Date timestamp, byte type, byte[] data) {
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

    public Date getTimestamp() {
        return timestamp;
    }

    public byte getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    public DataInputStream getDataInputStream() {
        return new DataInputStream(new ByteArrayInputStream(getData()));
    }
}
