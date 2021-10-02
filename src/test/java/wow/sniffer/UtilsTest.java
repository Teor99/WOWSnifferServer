package wow.sniffer;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilsTest {

    @Test
    public void revertLong() {
        assertEquals(Utils.revertLong(0x0513B67300000000L), 0x0000000073B61305L);
    }

    @Test
    public void revertShort() {
        assertEquals(Utils.revertShort((short) 0x0513), (short)0x1305);
    }
}