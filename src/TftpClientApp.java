import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import network.TftpClient;
import network.TftpClientStatus;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * client for TFTP
 */
public class TftpClientApp extends Application {
    /**
     * app window width
     */
    private static final double WINDOW_WIDTH = 400;

    /**
     * app window height
     */
    private static final double WINDOW_HEIGHT = 600;

    /**
     * the max count of the log textarea count
     */
    public static final int MAX_LOG_TEXTAREA_WORD_COUNT = 50000;

    /**
     * use to log.
     */
    private TextArea logTextArea;

    /**
     * server ip text field.
     */
    private TextField serverIpTextField;

    /**
     * base folder text field.
     */
    private TextField baseFolderTextField;

    private Button uploadBtn;

    private Button downloadBtn;

    private Button chooseFolderBtn;

    private Stage stage;

    /**
     * the tftp client deal with the core of tftp.
     */
    private TftpClient tftpClient;

    private StringBuilder logBuffer = new StringBuilder();

    private volatile Timer logTimer;

    private volatile String logText = "";

    @Override
    public void start(Stage stage) throws Exception {


        tftpClient = new TftpClient(this::printlnLogMsg, this::listenToClientStatus);
        //when the window close, release the resources.
        stage.setOnCloseRequest(this::dispose);

        this.stage = stage;
        VBox root = new VBox();
        root.setSpacing(5);
        //add all children panes.
        root.getChildren().addAll(buildServerConfigPane(), buildBaseFileConfigPane(), buildControlBtnPane(), buildLogPane());

        stage.setTitle("XXX's TFTP Client");
        stage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
        stage.setResizable(false);

        stage.show();
    }

    /**
     * listen to the status change
     * @param status
     */
    private void listenToClientStatus(TftpClientStatus status){
        switch (status){
            case DEALING:
                uploadBtn.setDisable(true);
                downloadBtn.setDisable(true);
                chooseFolderBtn.setDisable(true);
                break;
            case READY:
                uploadBtn.setDisable(false);
                downloadBtn.setDisable(false);
                chooseFolderBtn.setDisable(false);
                break;
        }
    }

    /**
     * release the resources.
     */
    private void dispose(WindowEvent event){
        try{
            tftpClient.dispose();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * prinltn the log msg to ui.
     * @param logMsg
     */
    private void printlnLogMsg(String logMsg){
        addLog(logMsg);
        //if many msg need to log at a short time, the application with be every slow,
        //so use the timer to after 0.2s update the log.
        if(logTimer == null){
            logTimer = new Timer("logTimer", true);
            logTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    logText += getLogBufferStr();
                    if(logText.length() > MAX_LOG_TEXTAREA_WORD_COUNT){
                        logText = logText.substring(logText.length() - MAX_LOG_TEXTAREA_WORD_COUNT);
                    }

                    Platform.runLater(() ->{
                        logTextArea.setText(logText);
                        logTextArea.setScrollTop(Double.MAX_VALUE);
                    });
                    logTimer = null;
                }
            }, 200);
        }
    }

    private synchronized void addLog(String logMsg){
        logBuffer.append(logMsg);
        logBuffer.append("\n");
    }

    private synchronized String getLogBufferStr(){
        String result = logBuffer.toString();
        logBuffer.setLength(0);
        return result;
    }

    /**
     * build the pane of control button.
     * @return
     */
    private Pane buildControlBtnPane() {
        HBox pane = new HBox();
        pane.setSpacing(5);

        uploadBtn = new Button("Upload");
        uploadBtn.setOnAction(event -> {
            //check and get the server ip.
            String serverIp = checkAndGetIpInput();
            if(serverIp == null){
                return;
            }

            //choose the file to upload.
            FileChooser fileChooser = getFileChooserWithBaseDir();
            File file = fileChooser.showOpenDialog(stage);
            if(file == null){
                showMsg("Please choose file!");
                return;
            }

            //get the remote file name.
            String fileName = getInputFileName();
            if(isStringEmpty(fileName)){
                showMsg("Please enter remote file name!");
                return;
            }

            if(fileName.contains("\\") || fileName.contains("/")){
                showMsg("File name can not contain '/' or '\\'!");
                return;
            }

            //upload the file
            tftpClient.upLoadFileAsyn(serverIp, file, fileName);
        });


        downloadBtn = new Button("Download");
        downloadBtn.setOnAction(event -> {
            //check and get the server ip.
            String serverIp = checkAndGetIpInput();
            if(serverIp == null){
                return;
            }

            //get the remote file name.
            String fileName = getInputFileName();
            if(isStringEmpty(fileName)){
                showMsg("Please enter remote file name!");
                return;
            }

            if(fileName.contains("\\") || fileName.contains("/")){
                showMsg("File name can not contain '/' or '\\'!");
                return;
            }

            //choose the file to upload.
            FileChooser fileChoose = getFileChooserWithBaseDir();
            File file = fileChoose.showSaveDialog(stage);
            if(file == null){
                showMsg("Please choose file!");
                return;
            }

            //upload the file
            tftpClient.downloadFileAsyn(serverIp, file, fileName);
        });




        pane.getChildren().addAll(uploadBtn, downloadBtn);
        return pane;
    }

    /**
     * check the str is empty or not.
     * @param str
     * @return
     */
    private boolean isStringEmpty(String str){
        return null == str || str.length() == 0;
    }

    /**
     * get the use input file name.
     */
    private String getInputFileName() {
        TextInputDialog textInputDialog = new TextInputDialog("");
        textInputDialog.setTitle("Remote file name?");
        textInputDialog.setHeaderText("Remote file name?");
        textInputDialog.setContentText("Enter remote file name: ");
        return textInputDialog.showAndWait().orElse("");
    }

    /**
     * init the fileChooser with init dir.
     */
    private FileChooser getFileChooserWithBaseDir(){
        FileChooser fileChooser = new FileChooser();

        String baseFileDir = baseFolderTextField.getText();
        if(baseFileDir.length() == 0){
            return fileChooser;
        }

        fileChooser.setInitialDirectory(new File(baseFileDir));
        return fileChooser;
    }



    /**
     * check ip is empty or not.
     * @return if ip is empty then alert and return null.
     */
    private String checkAndGetIpInput(){
        String ip = serverIpTextField.getText().trim();
        if(ip.length() == 0){
            showMsg("Please input server ip.");
            return null;
        }

        return ip;
    }

    /**
     * build the base file dir config pane.
     * @return
     */
    private Pane buildBaseFileConfigPane() throws IOException {
        VBox pane = new VBox();
        pane.setSpacing(5);

        chooseFolderBtn = new Button("Choose Folder");
        chooseFolderBtn.setOnAction( event -> {
            //select base dir.
            File file = new DirectoryChooser().showDialog(stage);
            if (file == null) {
                return;
            }

            baseFolderTextField.setText(file.getAbsolutePath());
        });

        //the base file dir
        baseFolderTextField = new TextField();
        baseFolderTextField.setEditable(false);
        baseFolderTextField.setFont(Font.font("MONOSPACED", FontWeight.NORMAL, baseFolderTextField.getFont().getSize()));
        //listener the text change to reset the column count.
        baseFolderTextField.textProperty().addListener(e ->{
            int count = Integer.max(20, baseFolderTextField.getText().length());
            baseFolderTextField.setPrefColumnCount(count);
        });

        //make scrollable
        ScrollPane sp = new ScrollPane();
        sp.setContent(baseFolderTextField);


        //initial the cur directory to display.
        baseFolderTextField.setText(new File(".").getCanonicalPath());

        pane.getChildren().addAll(chooseFolderBtn, sp);
        return pane;
    }

    /**
     * show the alert msg.
     * @param msg
     */
    private void showMsg(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Message");
        alert.setHeaderText("Message");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * build the server config pane.
     * @return
     */
    private Pane buildServerConfigPane() {
        HBox pane = new HBox();
        pane.setSpacing(5);

        //the log tile.
        Label title = new Label("Server:");

        serverIpTextField = new TextField("127.0.0.1");
        serverIpTextField.setPrefWidth(WINDOW_WIDTH - 50);

        pane.getChildren().addAll(title, serverIpTextField);
        return pane;
    }

    /**
     * build the log pane.
     *
     * @return log pane.
     */
    private Pane buildLogPane() {
        VBox pane = new VBox();
        pane.setSpacing(5);

        //the log tile.
        Label title = new Label("Log:");

        logTextArea = new TextArea();
        //fill the remain blank.
        logTextArea.setPrefWidth(WINDOW_WIDTH);
        logTextArea.setPrefHeight(WINDOW_HEIGHT);
        //if the line becomes too long to fit, the line is wrapped on a word boundary.
        logTextArea.setWrapText(true);

        pane.getChildren().addAll(title, logTextArea);
        return pane;
    }

    public static void main(String[] args) {
        //start the app.
        launch(args);
    }
}
