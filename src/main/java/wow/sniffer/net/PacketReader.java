package wow.sniffer.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class PacketReader {
    private final DataInputStreamReader disr;

    public PacketReader(InputStream inputStream) {
        this.disr = new DataInputStreamReader(inputStream);
    }

    public Packet readPacket() throws IOException {
        int packetOpcode = disr.readIntE();
        int packetSize = disr.readIntE();
        Date timestamp = new Date((long) disr.readIntE() * 1000);
        byte packetType = disr.readByte();
        byte[] packetData = null;

        if (packetSize > 0) {
            packetData = new byte[packetSize];
            for (int i = 0; i < packetData.length; i++) {
                packetData[i] = disr.readByte();
            }
        }

        return new Packet(packetOpcode, packetSize, timestamp, packetType, packetData);
    }

}
