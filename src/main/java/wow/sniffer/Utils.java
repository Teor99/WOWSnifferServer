package wow.sniffer;

public class Utils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static int revertInt(int i) {
        int b1 = (i & 0xFF);
        b1 <<= 24;
        b1 &= 0xFF000000;

        int b2 = (i & 0xFF00);
        b2 <<= 8;
        b2 &= 0xFF0000;

        int b3 = (i & 0xFF0000);
        b3 >>= 8;
        b3 &= 0xFF00;

        int b4 = (i & 0xFF000000);
        b4 >>= 24;
        b4 &= 0xFF;

        return b1 | b2 | b3 | b4;
    }
}
