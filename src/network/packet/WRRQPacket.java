package network.packet;

import utils.DataUtils;

import java.net.DatagramPacket;

/**
 * write and read data request packet
 */
public class WRRQPacket extends TftpPacket {
    /**
     * server file name
     */
    private String fileName;

    /**
     * write mode.
     */
    private String mode;

    private byte[] packetDatas;

    public WRRQPacket(String ip, int port, short opCode, String fileName, String mode){
        super(ip, port, opCode);
        this.fileName = fileName;
        this.mode = mode;
        initPacketDatas();
    }


    public WRRQPacket(DatagramPacket datagramPacket){
        super(datagramPacket);
        byte[] data = datagramPacket.getData();
        int fileNameEndIndex = -1;
        for (int i = 3; i < data.length; i++) {
           if(data[i] == 0){
               fileNameEndIndex = i;
               break;
           }
        }

        this.fileName = new String(data, 2, fileNameEndIndex - 2);
        int modeStartIndex = fileNameEndIndex + 1;
        this.mode = new String(data, modeStartIndex, datagramPacket.getLength() - modeStartIndex - 1);
        initPacketDatas();
    }

    @Override
    protected byte[] getPacketDatas() {
        return packetDatas;
    }


    /**
     * init packet datas.
     */
    protected void initPacketDatas(){
        byte[] fileNameBytes = fileName.getBytes();
        byte[] modeBytes = mode.getBytes();

        int dataByteCount = 4 + fileNameBytes.length + modeBytes.length;
        packetDatas = new byte[dataByteCount];

        DataUtils.shortToBytes(packetDatas, getOpCode(), 0);
        System.arraycopy(fileNameBytes, 0, packetDatas, 2,fileNameBytes.length);
        // terminated by a ‘0’ byte
        packetDatas[2 + fileNameBytes.length] = 0;
        System.arraycopy(modeBytes, 0, packetDatas, 3 + fileNameBytes.length, modeBytes.length);
        // terminated by a ‘0’ byte
        packetDatas[packetDatas.length - 1] = 0;
    }

    @Override
    protected int getPacketLength() {
        return packetDatas.length;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMode() {
        return mode;
    }
}
