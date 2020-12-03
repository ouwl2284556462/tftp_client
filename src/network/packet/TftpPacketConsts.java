package network.packet;


public interface TftpPacketConsts {

    /**
     * Read request opcode
     */
    public static final short OP_RRQ = 1;

    /**
     * Write request opcode
     */
    public static final short OP_WRQ = 2;

    /**
     * ack opcode
     */
    public static final short OP_ACK = 4;

    /**
     * data opcode.
     */
    public static final short OP_DATA = 3;

    /**
     * error opcode.
     */
    public static final short OP_ERROR = 5;


    /**
     * Undefined error
     */
    public static final short ERRCODE_UNDEF = 0;

    /**
     * File not found
     */
    public static final short ERRCODE_NOTFD = 1;

    /**
     * Access violation (cannot open file)
     */
    public static final short ERRCODE_ACCESS = 2;

    /**
     * Illegal Opcode
     */
    public static final short ERRCODE_ILLOP = 4;

    /**
     * request mode.
     */
    public static final String MODE_OCTET = "octet";
}
