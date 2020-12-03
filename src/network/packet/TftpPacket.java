package network.packet;

import utils.DataUtils;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

/**
 * the tftp packet
 */
public abstract class TftpPacket {
    /**
     * ip info
     */
    private String ip;

    /**
     * port info
     */
    private int port;

    /**
     * opcode
     */
    private short opCode;

    public TftpPacket(String ip, int port, short opCode) {
        this.opCode = opCode;
        this.ip = ip;
        this.port = port;
    }

    public TftpPacket(DatagramPacket datagramPacket) {
        short opCode = DataUtils.bytesToShort(datagramPacket.getData(), 0);
        this.opCode = opCode;
        this.ip = datagramPacket.getAddress().getHostAddress();
        this.port = datagramPacket.getPort();
    }

    /**
     * build get datagarmPacket.
     *
     * @return
     */
    public DatagramPacket build() {
        return new DatagramPacket(getPacketDatas(), getPacketLength(), new InetSocketAddress(ip, port));
    }


    /**
     * get the packet byte datas.
     *
     * @return
     */
    protected abstract byte[] getPacketDatas();

    /**
     * get the packet length.
     * @return
     */
    protected abstract int getPacketLength();

    /**
     * get the opCode.
     *
     * @return
     */
    public short getOpCode() {
        return opCode;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

}
