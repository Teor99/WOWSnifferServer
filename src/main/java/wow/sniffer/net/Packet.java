package wow.sniffer.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

public class Packet {

    private final int opcode;
    private final int size;
    private final Date timestamp;
    private final byte type;
    private DataInputStreamReader data;

    public Packet(int opcode, int size, Date timestamp, byte type, byte[] data) {
        this.opcode = opcode;
        this.size = size;
        this.timestamp = timestamp;
        this.type = type;
        if (data != null) {
            this.data = new DataInputStreamReader(new ByteArrayInputStream(data));
        }
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
            try {
                sb.append("data: ").append(data.bytesToHexString());
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    public int readIntE() throws IOException {
        return data.readIntE();
    }

    public byte readByte() throws IOException {
        return data.readByte();
    }

    public short readShortE() throws IOException {
        return data.readShortE();

    }

    public long readLongE() throws IOException {
        return data.readLongE();
    }

    public String readCString() throws IOException {
        StringBuilder sb = new StringBuilder();

        while (true) {
            byte b = data.readByte();
            if (b == 0) break;
            sb.append((char) b);
        }

        return sb.toString();
    }

    public void skip(int i) throws IOException {
        data.skip(i);
    }
}
