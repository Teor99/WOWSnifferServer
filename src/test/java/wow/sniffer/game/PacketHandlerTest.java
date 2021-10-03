package wow.sniffer.game;

import org.junit.Test;
import wow.sniffer.net.PacketHandler;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class PacketHandlerTest {

    @Test
    public void processInputStream1() throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream("C:\\Users\\Maxi\\source\\repos\\hookLib\\Debug\\wowsniff_12340_1632836129_2021y09m28d18h35i29s.bin"))) {
            PacketHandler packetHandler = new PacketHandler();
            packetHandler.processInputStream(dis);
        }
    }
}