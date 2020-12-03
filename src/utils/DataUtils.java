package utils;

/**
 * the utils for data change.
 */
public class DataUtils {
    /**
     * short change to byte array.
     * @param num
     * @returnDataUtils
     */
    public static byte[] shortToBytes(short num) {
        byte b[] = new byte[2];
        b[0] = (byte) num;
        b[1] = (byte) (num >>> 8) ;
        return b;
    }


    /**
     * change short to byte and write into the bytes.
     * @param bytes
     * @param num
     * @param start
     */
    public static void shortToBytes(byte[] bytes, short num, int start) {
        bytes[start] = (byte) num;
        bytes[start + 1] = (byte) (num >>> 8) ;
    }

    /**
     * bytes to short
     * @param bytes
     * @return
     */
    public static short bytesToShort(byte[] bytes) {
        return (short) (bytes[0]& 0xff | (bytes[1] << 8));
    }

    /**
     * bytes to short
     * @param bytes
     * @param start start index
     * @return
     */
    public static short bytesToShort(byte[] bytes, int start) {
        return (short) (bytes[start]& 0xff | (bytes[start + 1] << 8));
    }
}
