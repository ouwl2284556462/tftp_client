package network.packet;

import utils.DataUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.util.function.BooleanSupplier;

public class DATAPacket extends TftpPacket {

    /**
     * byte count of data
     */
    private final static int DATA_SIZE = 512;


    private byte[] buffer;

    /**
     * the count of data read from inputstream.
     */
    private int readSize = 0;

    public DATAPacket(String ip, int port) {
        super(ip, port, TftpPacketConsts.OP_DATA);
        buffer = new byte[DATA_SIZE + 4];
    }

    public DATAPacket(DatagramPacket datagramPacket) {
        super(datagramPacket);
        buffer = new byte[datagramPacket.getLength()];
        System.arraycopy(datagramPacket.getData(), 0, buffer, 0, buffer.length);
    }



    /**
     * read data from input stream.
     * @param inputStream
     * @return
     * @throws IOException
     */
    public BooleanSupplier readBlockData(InputStream inputStream){
        DataUtils.shortToBytes(buffer, getOpCode(), 0);
        short[] block = {1};
        boolean[] isFinish = {false};
        return () -> {
            try {
                if(isFinish[0]){
                    return false;
                }

                //write block number.
                DataUtils.shortToBytes(buffer, block[0], 2);
                //read data from inputstream.
                int count = inputStream.read(buffer, 4, DATA_SIZE);
                if(count < 0){
                    if(readSize != DATA_SIZE){
                        return false;
                    }

                    //If the entire data file to be transferred is dividable by 512,
                    //the last packet contains 0 data bytes (an empty packet)
                    count = 0;
                    isFinish[0] = true;
                }

                readSize = count;
                ++block[0];
                if(block[0] >= Short.MAX_VALUE){
                    block[0] = 1;
                }

                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    protected byte[] getPacketDatas() {
        return buffer;
    }

    @Override
    protected int getPacketLength() {
        return readSize + 4;
    }

    /**
     * get blockNum
     */
    public short getBlockNum(){
        return DataUtils.bytesToShort(buffer, 2);
    }

    /**
     * write block data to outputstream
     * @param outputStream
     */
    public void writeBlockData(OutputStream outputStream) throws IOException {
        if(buffer.length <= 4){
            return;
        }
        //read data from inputstream.
        outputStream.write(buffer, 4, buffer.length - 4);
    }


    /**
     * check if packet is last or not
     * @return
     */
    public boolean isLast(){
        return buffer.length < DATA_SIZE + 4;
    }

}
