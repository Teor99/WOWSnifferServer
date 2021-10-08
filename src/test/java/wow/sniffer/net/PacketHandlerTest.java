package wow.sniffer.net;

import org.junit.Test;

import static org.junit.Assert.*;

public class PacketHandlerTest {

    @Test
    public void costToString() {
        assertEquals("10s", PacketHandler.costToString(1000));
        assertEquals("10c", PacketHandler.costToString(10));
        assertEquals("1s23c", PacketHandler.costToString(123));
        assertEquals("1g", PacketHandler.costToString(10000));
        assertEquals("1g23c", PacketHandler.costToString(10023));
    }
}