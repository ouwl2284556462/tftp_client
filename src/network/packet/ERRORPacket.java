package network.packet;

import utils.DataUtils;

import java.net.DatagramPacket;

/**
 * err msg
 */
public class ERRORPacket extends TftpPacket {

    private short errCode;

    private String errMsg;

    private byte[] buffer;


    public ERRORPacket(String ip, int port, short errCode, String errMsg) {
        super(ip, port, TftpPacketConsts.OP_ERROR);
        this.errCode = errCode;
        this.errMsg = errMsg;
        init();
    }

    private void init() {
        byte[] errMsgBytes = errMsg.getBytes();
        buffer = new byte[errMsgBytes.length + 5];
        DataUtils.shortToBytes(buffer, getOpCode(), 0);
        DataUtils.shortToBytes(buffer, errCode, 2);
        System.arraycopy(errMsgBytes, 0, buffer, 4, errMsgBytes.length);
    }

    public ERRORPacket(DatagramPacket datagramPacket) {
        super(datagramPacket);
        buffer = datagramPacket.getData();
        this.errCode = DataUtils.bytesToShort(buffer, 2);
        this.errMsg = new String(buffer, 4, datagramPacket.getLength() - 5);
    }

    @Override
    protected byte[] getPacketDatas() {
        return buffer;
    }

    @Override
    protected int getPacketLength() {
        return buffer.length;
    }

    public short getErrCode() {
        return errCode;
    }

    public String getErrMsg() {
        return errMsg;
    }
}
