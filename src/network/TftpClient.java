package network;

import network.packet.*;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * the tftp client deal with the core of tftp.
 */
public class TftpClient {

    /**
     * the port for receive request.
     */
    private static final int SERVER_PORT = 69;


    /**
     * the time out of receive.
     */
    private static final int RECEIVE_TIMEOUT = 5000;

    /**
     * buffer max size.
     */
    private static final int BUFFER_MAX_LENGTH = 1024;

    /**
     * use to log info.
     */
    private Consumer<String> logger;

    /**
     * the listener of status.
     */
    private Consumer<TftpClientStatus> statusListener;

    private ExecutorService threadpool;

    /**
     * the retry time of when meet the error data
     */
    private static final int RETRY_TIME = 4;

    /**
     * the run status of client.
     */
    private boolean run;


    /**
     *
     * @param logger use to log the information.
     * @param statusListener use to listen to the status change.
     */
    public TftpClient(Consumer<String> logger, Consumer<TftpClientStatus> statusListener){
        threadpool = Executors.newCachedThreadPool();
        this.logger = logger;
        this.statusListener = statusListener;
        run = true;
    }


    /**
     * release all the resources.
     */
    public void dispose(){
        run = false;
        try{
            if(threadpool != null){
                threadpool.shutdownNow();
            }
        }catch (Exception e){
            e.printStackTrace();
            logger.accept("Dispose error:" + e.getMessage());
        }
    }

    /**
     * upload file.
     * @param ip server ip.
     * @param file the file to upload.
     * @param remoteFileName the file name of server.
     */
    public void upLoadFileAsyn(String ip, File file, String remoteFileName) {
        statusListener.accept(TftpClientStatus.DEALING);
        logger.accept(String.format("Upload: %s -> %s", file.getName(), remoteFileName));
        //start the upload task.
        threadpool.execute(() ->{
            try(DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(RECEIVE_TIMEOUT);

                WRRQPacket wrrqPacket = TftpPacketFactory.buildWRQPacket(ip, SERVER_PORT, remoteFileName, TftpPacketConsts.MODE_OCTET);
                logger.accept(String.format("Upload:Send request WRQ<%s> Mode<%s>", wrrqPacket.getOpCode(), wrrqPacket.getMode()));
                //build the wrqPacket.
                socket.send(wrrqPacket.build());

                //get response from server.
                byte[] buffer = new byte[BUFFER_MAX_LENGTH];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);


                int retryCount = 0;
                TftpPacket tftpPacket = null;
                while(true){
                    try{
                        socket.receive(responsePacket);
                        tftpPacket = TftpPacketFactory.buildTftpPacket(responsePacket);
                        short opCode = tftpPacket.getOpCode();
                        if(opCode == TftpPacketConsts.OP_ERROR){
                            //error.
                            ERRORPacket errP = (ERRORPacket)tftpPacket;
                            throw new RuntimeException(String.format("errcode:%s, errMsg:%s", errP.getErrCode(), errP.getErrMsg()));
                        }


                        if(opCode != TftpPacketConsts.OP_ACK){
                            logger.accept("Upload:opcode err:" + opCode);
                            logger.accept("Ignore err packet...");
                            continue;
                        }

                        break;
                    } catch (SocketTimeoutException e) {
                        e.printStackTrace();
                        retryCount = checkRetry(retryCount, "Upload:Receive time out");
                    }
                }

                logger.accept(String.format("Upload:Receive response opcode:%s(%s), blockNo:%s", TftpPacketConsts.OP_ACK, "ACK", ((ACKPacket) tftpPacket).getBlockNo()));
                //upload the file data.
                logger.accept(String.format("Upload:Open file:%s", file.getAbsolutePath()));
                try(BufferedInputStream bi = new BufferedInputStream(new FileInputStream(file))){
                    DATAPacket dataPacket = TftpPacketFactory.buildDatapacket(tftpPacket);

                    //start read the data from file.
                    BooleanSupplier dataReader = dataPacket.readBlockData(bi);
                    while(dataReader.getAsBoolean() && run){
                        retryCount = 0;
                        while(true){
                            //send to server.
                            logger.accept(String.format("Upload:Send data packet:%s(%s), blockNo:%s", TftpPacketConsts.OP_DATA, "DATA", dataPacket.getBlockNum()));

                            socket.send(dataPacket.build());
                            //getResponse.
                            try{
                                while (true){
                                    socket.receive(responsePacket);

                                    TftpPacket response = TftpPacketFactory.buildTftpPacket(responsePacket);
                                    if(response.getOpCode() == TftpPacketConsts.OP_ERROR){
                                        //error.
                                        ERRORPacket errP = (ERRORPacket)response;
                                        throw new RuntimeException(String.format("errcode:%s, errMsg:%s", errP.getErrCode(), errP.getErrMsg()));
                                    }

                                    if(response.getOpCode() != TftpPacketConsts.OP_ACK){
                                        logger.accept("Upload:opcode err:" + response.getOpCode());
                                        logger.accept("Ignore err packet...");
                                        continue;
                                    }

                                    short responseBlockNo = ((ACKPacket) response).getBlockNo();
                                    logger.accept(String.format("Upload:Receive response opcode:%s(%s), blockNo:%s", TftpPacketConsts.OP_ACK, "ACK", responseBlockNo));
                                    if(responseBlockNo != dataPacket.getBlockNum()){
                                        logger.accept(String.format("Upload:block number err:cur:%s, expect:%s", responseBlockNo, dataPacket.getBlockNum()));
                                        logger.accept("Ignore err packet...");
                                        continue;
                                    }

                                    break;
                                }

                            } catch (SocketTimeoutException e) {
                                e.printStackTrace();
                                retryCount = checkRetry(retryCount, "Upload:Receive time out");
                                continue;
                            }

                            //if no error happen, then do next
                            break;
                        }

                    }
                }


                logger.accept(String.format("Upload:Finish file<%s> -> server file<%s>", file.getAbsolutePath() + File.separator + file.getName(), remoteFileName));
            } catch (Exception e) {
                e.printStackTrace();
                logger.accept("Upload:err:" + e.getMessage());
            }finally {
                statusListener.accept(TftpClientStatus.READY);
            }
        });
    }

    private int checkRetry(int retryCount, String errMsg) {
        ++retryCount;
        if(retryCount > RETRY_TIME){
            throw new RuntimeException(errMsg);
        }

        logger.accept(errMsg);
        logger.accept(String.format("Retrying:retry count:%s...", retryCount));
        return retryCount;
    }

    /**
     * download file from server
     * @param serverIp
     * @param file
     * @param serverFileName
     */
    public void downloadFileAsyn(String serverIp, File file, String serverFileName) {
        statusListener.accept(TftpClientStatus.DEALING);
        logger.accept(String.format("Download: %s -> %s", serverFileName, file.getName()));
        //start the download task.
        threadpool.execute(() ->{
            try(DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(RECEIVE_TIMEOUT);

                WRRQPacket wrrqPacket = TftpPacketFactory.buildRRQPacket(serverIp, SERVER_PORT, serverFileName, TftpPacketConsts.MODE_OCTET);
                logger.accept(String.format("Download:Send request RRQ<%s> Mode<%s>", wrrqPacket.getOpCode(), wrrqPacket.getMode()));
                //build the wrqPacket.
                socket.send(wrrqPacket.build());

                //get response from server.
                byte[] buffer = new byte[BUFFER_MAX_LENGTH];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);


                try(BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(file))){

                    short blockNum = 1;
                    boolean isFinish = false;

                    ACKPacket ackPacket = null;

                    while (true){

                        int retryCount = 0;
                        while(true){
                            try{
                                socket.receive(responsePacket);
                                TftpPacket tftpPacket = TftpPacketFactory.buildTftpPacket(responsePacket);
                                short opCode = tftpPacket.getOpCode();
                                if(opCode == TftpPacketConsts.OP_ERROR){
                                    //error.
                                    ERRORPacket errP = (ERRORPacket)tftpPacket;

                                    throw new RuntimeException(String.format("errcode:%s, errMsg:%s", errP.getErrCode(), errP.getErrMsg()));
                                }


                                if(opCode != TftpPacketConsts.OP_DATA){
                                    logger.accept("Download:opcode err:" + opCode);
                                    logger.accept("Ignore err packet...");
                                    continue;
                                }

                                DATAPacket dataPacket = (DATAPacket)tftpPacket;
                                logger.accept(String.format("Download:Receive blockNo:%s", dataPacket.getBlockNum()));
                                if(blockNum != dataPacket.getBlockNum()){
                                    logger.accept(String.format("Download:block number err:cur:%s, expect:%s", dataPacket.getBlockNum(), blockNum));
                                    logger.accept("Ignore err packet...");
                                    continue;
                                }

                                //save data to file.
                                dataPacket.writeBlockData(bo);
                                if(dataPacket.isLast()){
                                    isFinish = true;
                                }

                                if(ackPacket == null){
                                    ackPacket = TftpPacketFactory.buildACKPacket(dataPacket, blockNum);
                                }

                                break;
                            } catch (SocketTimeoutException e) {
                                e.printStackTrace();
                                retryCount = checkRetry(retryCount, "Download:Receive time out");
                            }
                        }


                        logger.accept(String.format("Download:Send ACK, blockNo:%s", ackPacket.getBlockNo()));
                        socket.send(ackPacket.build());

                        if(isFinish){
                            break;
                        }

                        ++blockNum;
                        if(blockNum >= Short.MAX_VALUE){
                            blockNum = 1;
                        }
                        //reset the block number.
                        ackPacket.resetBlockNo(blockNum);
                    }

                    bo.flush();
                }

                logger.accept(String.format("Download:Finish file<%s> -> server file<%s>", file.getName(), serverFileName));
            } catch (Exception e) {
                e.printStackTrace();
                file.delete();
                logger.accept("Download:err:" + e.getMessage());
            }finally {
                statusListener.accept(TftpClientStatus.READY);
            }
        });
    }
}
