package wow.sniffer;

import org.junit.Test;
import wow.sniffer.net.DataInputStreamReader;

import static org.junit.Assert.*;

public class UtilsTest {

    @Test
    public void revertLong() {
        assertEquals(DataInputStreamReader.revertLong(0x0513B67300000000L), 0x0000000073B61305L);
    }

    @Test
    public void revertShort() {
        assertEquals(DataInputStreamReader.revertShort((short) 0x0513), (short)0x1305);
    }
}