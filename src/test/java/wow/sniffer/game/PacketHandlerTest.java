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
            PacketHandler packetHandler = new PacketHandler(dis);
            packetHandler.processInputStream();
        }
    }

    @Test
    public void processInputStream2() throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream("C:\\Users\\Maxi\\source\\repos\\hookLib\\Debug\\wowsniff_12340_1632833993_2021y09m28d17h59i53s.bin"))) {
            PacketHandler packetHandler = new PacketHandler(dis);
            packetHandler.processInputStream();
        }
    }

    @Test
    public void processInputStream3() throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream("C:\\Users\\Maxi\\source\\repos\\hookLib\\Debug\\wowsniff_12340_1632888371_2021y09m29d09h06i11s.bin"))) {
            PacketHandler packetHandler = new PacketHandler(dis);
            packetHandler.processInputStream();
        }
    }

    @Test
    public void processInputStream4() throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream("C:\\Users\\Maxi\\source\\repos\\hookLib\\Debug\\wowsniff_12340_1633002184_2021y09m30d16h43i04s.bin"))) {
            PacketHandler packetHandler = new PacketHandler(dis);
            packetHandler.processInputStream();
        }
    }

    @Test
    public void processInputStream5() throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream("C:\\Users\\Maxi\\source\\repos\\hookLib\\Debug\\wowsniff_12340_1633003669_2021y09m30d17h07i49s.bin"))) {
            PacketHandler packetHandler = new PacketHandler(dis);
            packetHandler.processInputStream();
        }
    }

    @Test
    public void processInputStream6() throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream("C:\\Users\\Maxi\\source\\repos\\hookLib\\Debug\\wowsniff_12340_1633021318_2021y09m30d22h01i58s.bin"))) {
            PacketHandler packetHandler = new PacketHandler(dis);
            packetHandler.processInputStream();
        }
    }

    @Test
    public void processInputStream7() throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream("C:\\Users\\Maxi\\source\\repos\\hookLib\\Debug\\wowsniff_12340_1633054192_2021y10m01d07h09i52s.bin"))) {
            PacketHandler packetHandler = new PacketHandler(dis);
            packetHandler.processInputStream();
        }
    }
}