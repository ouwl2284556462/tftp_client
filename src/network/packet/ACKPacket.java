package network.packet;

import utils.DataUtils;

import java.net.DatagramPacket;

/**
 * ACK packet
 */
public class ACKPacket extends TftpPacket {

    /**
     * ack the data block number.
     */
    private short blockNo;

    /**
     * packet data.
     */
    private byte[] buffer;

    public ACKPacket(String ip, int port, short blockNo){
        super(ip, port, TftpPacketConsts.OP_ACK);
        this.blockNo = blockNo;
        initBuffer();
    }

    public ACKPacket(DatagramPacket datagramPacket) {
        super(datagramPacket);
        this.blockNo = DataUtils.bytesToShort(datagramPacket.getData(), 2);
        initBuffer();
    }

    /**
     * init buffer.
     */
    private void initBuffer(){
        buffer = new byte[4];
        //write opcode
        DataUtils.shortToBytes(buffer, getOpCode(), 0);
        //write blockno
        DataUtils.shortToBytes(buffer, blockNo, 2);
    }

    public short getBlockNo() {
        return blockNo;
    }

    /**
     * reset the block number.
     */
    public void resetBlockNo(short blockNo){
        this.blockNo = blockNo;
        //write blockno
        DataUtils.shortToBytes(buffer, blockNo, 2);
    }

    @Override
    protected byte[] getPacketDatas() {
        return buffer;
    }

    @Override
    protected int getPacketLength() {
        return buffer.length;
    }
}
