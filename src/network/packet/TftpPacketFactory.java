package network.packet;

import utils.DataUtils;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;

/**
 * use to build the TftpPacket.
 */
public class TftpPacketFactory {


    /**
     * build the WRQ packet.
     *
     * @param ip
     * @param port
     * @param fileName
     * @param mode
     * @return
     */
    public static WRRQPacket buildWRQPacket(String ip, int port, String fileName, String mode) throws UnknownHostException, UnsupportedEncodingException {
        return new WRRQPacket(ip, port, TftpPacketConsts.OP_WRQ, fileName, mode);
    }

    /**
     * build the RRQ packet.
     *
     * @param ip
     * @param port
     * @param fileName
     * @param mode
     * @return
     */
    public static WRRQPacket buildRRQPacket(String ip, int port, String fileName, String mode) throws UnknownHostException, UnsupportedEncodingException {
        return new WRRQPacket(ip, port, TftpPacketConsts.OP_RRQ, fileName, mode);
    }

    /**
     * build the ACK packet
     * @param ip
     * @param port
     * @param blockNo
     * @return
     */
    public static ACKPacket buildACKPacket(String ip, int port, short blockNo){
        return new ACKPacket(ip, port, blockNo);
    }

    /**
     * build the ack packet from tarPacket
     * @param tarPacket
     * @param blockNo
     */
    public static ACKPacket buildACKPacket(TftpPacket tarPacket, short blockNo) {
        return new ACKPacket(tarPacket.getIp(), tarPacket.getPort(), blockNo);
    }

    /**
     * build data packet.
     * @param ip
     * @param port
     * @return
     */
    public static DATAPacket buildDatapacket(String ip, int port){
        return new DATAPacket(ip, port);
    }

    /**
     * build the data packet from tarPacket
     * @param tarPacket
     */
    public static DATAPacket buildDatapacket(TftpPacket tarPacket) {
        return new DATAPacket(tarPacket.getIp(), tarPacket.getPort());
    }

    /**
     * build the err packet from tarPacket
     * @param tarPacket
     */
    public static ERRORPacket buildERRORpacket(TftpPacket tarPacket, short errCode, String errMsg) {
        return new ERRORPacket(tarPacket.getIp(), tarPacket.getPort(), errCode, errMsg);
    }


    /**
     * build TftpPacket from datagramPacket.
     * @param datagramPacket
     * @return
     * @throws UnknownHostException
     */
    public static TftpPacket buildTftpPacket(DatagramPacket datagramPacket) throws UnknownHostException, UnsupportedEncodingException {
        short opCode = DataUtils.bytesToShort(datagramPacket.getData(), 0);

        if(TftpPacketConsts.OP_WRQ == opCode || TftpPacketConsts.OP_RRQ == opCode){
            return new WRRQPacket(datagramPacket);
        }

        if(TftpPacketConsts.OP_ACK == opCode){
            return new ACKPacket(datagramPacket);
        }

        if(TftpPacketConsts.OP_DATA == opCode){
            return new DATAPacket(datagramPacket);
        }

        if(TftpPacketConsts.OP_ERROR == opCode){
            return new ERRORPacket(datagramPacket);
        }

        return new ERRORPacket(datagramPacket.getAddress().getHostAddress(), datagramPacket.getPort(), TftpPacketConsts.ERRCODE_ILLOP, "Illegal Opcode");
    }


}
