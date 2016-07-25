package com.dudu.monitor.obdUpdate;

import com.dudu.android.libserial.SerialManager;
import com.dudu.commonlib.utils.File.FileUtil;
import com.dudu.commonlib.utils.thread.ThreadUtils;
import com.dudu.monitor.obdUpdate.config.ObdUpdateCmd;
import com.dudu.monitor.obdUpdate.config.ObdUpdateConstants;
import com.dudu.monitor.obdUpdate.jni.Ymodem;
import tm.dudu.ext.GpioControl;
import com.dudu.workflow.obd.OBDStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android_serialport_api.SerialPort;

/**
 * Created by dengjun on 2016/5/12.
 * Description :
 */
public class ObdUpdateUtils {
    public Logger log = LoggerFactory.getLogger("car.obdUpdate");

    private ObdUpdateCallback obdUpdateCallback = null;
    /* 升级完成后读取到的obdbin版本号*/
    private String updatedObdBinVersion = "";

    public void updateObdBin(String curObdBinVersion) {
        new Thread(() -> {
            log.info("updateObdBin-----------------");
            boolean updateResult = false;
            try {
                if (ObdUpdateConstants.hardUpdateObdBinVersion.equals(curObdBinVersion)) {
                    updateResult = doNativeHardUpdate();
                } else {
                    String obdBinPath = compareVersion(curObdBinVersion);
                    log.info("compareVersion结果：{}", obdBinPath);
                    if (ObdUpdateConstants.obdbinOnlineAbPath.equals(obdBinPath)) {
                        updateResult = doOnlineUpdate();
                    } else if (ObdUpdateConstants.obdbinNativeAbPath.equals(obdBinPath)) {
                        updateResult = doNativeUpdate();
                    } else if (obdBinPath == null) {
                        updateResult = false;
                        updatedObdBinVersion = curObdBinVersion;
                    }
                }

            } catch (Exception e) {
                log.error("异常", e);
            }
           /* if (updateResult){
                ObdUpdateCmd.setSerialPort();
                ObdUpdateCmd.resetObdChip();
            }*/
            if (obdUpdateCallback != null) {
                obdUpdateCallback.onResult(updateResult, updatedObdBinVersion);
            }

        }).start();
    }

    private void resetchip() {
        /** when the update is finished, the chip will sleep**/
        ObdUpdateCmd.resetObdChip();
        threadSleep(5500);
        GpioControl.wakeObd();
        threadSleep(1000);
    }

    private String compareVersion(String curObdBinVersion) {
        String downloadObdBinVersion = ObdBinFetchUtils.getSavedObdVersion();
        log.info("当前版本号：{}，launcher自带版本号：{}，下载的版本号：{}", curObdBinVersion, ObdUpdateConstants.OBD_CONFIG_VERSION, downloadObdBinVersion);
        if ("".equals(curObdBinVersion)) {
            return null;
        }
        long curObdBinVersionLong = ObdBinFetchUtils.obdVersionStringToLong(curObdBinVersion);
        long nativeConfigObdVersionLong = ObdBinFetchUtils.obdVersionStringToLong(ObdUpdateConstants.OBD_CONFIG_VERSION);
        long downloadObdBinLong = ObdBinFetchUtils.obdVersionStringToLong(downloadObdBinVersion);

        if (curObdBinVersionLong < nativeConfigObdVersionLong || curObdBinVersionLong < downloadObdBinLong) {
            if (nativeConfigObdVersionLong < downloadObdBinLong) {
                return ObdUpdateConstants.obdbinOnlineAbPath;
            } else {
                return ObdUpdateConstants.obdbinNativeAbPath;
            }
        } else {
            return null;
        }
    }

    private boolean doOnlineUpdate() {
        log.info("obd升级在线下载的obdBin");
        if (ObdBinFetchUtils.getDownloadState() == true && FileUtil.isFileExist(ObdUpdateConstants.obdbinOnlineAbPath)) {
            if (updateAction(ObdUpdateConstants.obdbinOnlineAbPath) == 0) {
                resetchip();
                updatedObdBinVersion = readObdBinVersion();
                ObdBinFetchUtils.saveDownloadState(false);
                FileUtil.deleteFile(ObdUpdateConstants.obdbinOnlineAbPath);
                return true;
            }
        }
        return false;
    }


    private boolean doNativeUpdate() {
        log.info("obd升级launcher自带的obdBin");
        if (ObdBinFetchUtils.getCopyFlag() == true && FileUtil.isFileExist(ObdUpdateConstants.obdbinNativeAbPath)) {
            if (updateAction(ObdUpdateConstants.obdbinNativeAbPath) == 0) {
                resetchip();
                updatedObdBinVersion = readObdBinVersion();
                return true;
            }
        }
        return false;
    }


    private boolean doNativeHardUpdate() {
        log.info("强制obd升级launcher的obdBin");
        if (FileUtil.isFileExist(ObdUpdateConstants.obdbinNativeAbPath)) {
            if (updateAction(ObdUpdateConstants.obdbinNativeAbPath) == 0) {
                resetchip();
                updatedObdBinVersion = readObdBinVersion();
                return true;
            }
        }
        return false;
    }

    private int updateAction(String obdBinAbPath) {
        readyToUpdate();
        enableUpdate();
        return sendObdBin(obdBinAbPath);
    }

    private void readyToUpdate() {
        log.info("准备升级");
        OBDStream.getInstance().obdStreamClose();
        ObdUpdateCmd.setSerialPort();
        ObdUpdateCmd.resetObdChip();
        threadSleep(1 * 1000);
        GpioControl.wakeObd();
    }

    private boolean enableUpdate() {
        boolean enableFlag = false;
        log.info("使能升级obd");
        SerialPort serialPort = SerialManager.getInstance().getSerialPortOBD();
        try {
            serialPort.getOutputStream().write(ObdUpdateCmd.DOWNLOAD_MODE.getBytes());
            serialPort.getOutputStream().flush();
            readByteWithTimeOut(serialPort.getInputStream(), 20);
            SerialManager.getInstance().closeSerialPortOBD();
            enableFlag = true;
            threadSleep(2 * 1000);
        } catch (IOException e) {
            log.error("异常", e);
        }
        return enableFlag;
    }

    private boolean getNode(String path){
        File mFile;

        mFile = new File(path); //device's nodefile.
        //use file.exists() 
        if( mFile.exists() ){
        	log.debug("OBD", path + " exists");
            return true;
        }else{
            log.debug("OBD", path + " NOT exists!");
            return false;
        }
    }

    private int sendObdBin(String obdBinAbPath) {
		Ymodem ymodem = new Ymodem();
        log.debug("OBD","发送obd固件"+obdBinAbPath);

		int sendResult = -1;
		if(getNode("/dev/ttyHSL1")){
			sendResult = ymodem.sendFileLowSpeedSerial(obdBinAbPath);
		}else{
			sendResult = ymodem.sendFile(obdBinAbPath);
		}

        log.debug("OBD","发送obdBin结果：{}"+ sendResult);
		return sendResult;
    }

    //会阻塞
    private String readObdBinVersion() {
        String obdVersion = "";
/*        SerialPort serialPort = SerialManager.getInstance().getSerialPortOBD();
        try {
//            serialPort.getOutputStream().write(("ATROFF" + "\r\n").getBytes(StandardCharsets.US_ASCII));
            serialPort.getOutputStream().write((ObdUpdateFlow.ATGETVER + "\r\n").getBytes(StandardCharsets.US_ASCII));
            serialPort.getOutputStream().flush();
            for (int i = 0; i < 3; i++) {
                obdVersion = readLineWithTimeOut(serialPort.getInputStream(), ObdUpdateFlow.ATGETVER, 10);
                log.info("读取的版本号：{}", obdVersion);
                if (!"".equals(obdVersion)) {
                    break;
                }
//                serialPort.getOutputStream().write(("ATROFF" + "\r\n").getBytes(StandardCharsets.US_ASCII));
                serialPort.getOutputStream().write((ObdUpdateFlow.ATGETVER + "\r\n").getBytes(StandardCharsets.US_ASCII));
                serialPort.getOutputStream().flush();
            }
            SerialManager.getInstance().closeSerialPortOBD();
        } catch (Exception e) {
            log.error("异常", e);
        }*/
        return obdVersion;
    }


    private String readLineWithTimeOut(InputStream inputStream, String stringToRead, int seconds) {
        boolean readRunFlag = true;
        String readedSting = "";
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        long timestamp = System.currentTimeMillis();
        try {
            while (readRunFlag) {
                String line = "";
                if (inputStream.available() > 0) {
                    while ((line = bufferedReader.readLine()) != null) {
                        log.debug("去读到一行字符串：{}", line);
                        if (line.contains(stringToRead)) {
                            readedSting = getVersion(line);
                            readRunFlag = false;
                            break;
                        }
                        ThreadUtils.threadSleep(10);
                        if ((System.currentTimeMillis() - timestamp) > seconds * 1000) {
                            readRunFlag = false;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("异常", e);
        }
        return readedSting;
    }

    private String getVersion(String line) {
        return line.substring(line.lastIndexOf("V") + 1).trim();
    }

    private void readByteWithTimeOut(InputStream inputStream, int seconds) throws IOException {
        boolean readRunFlag = true;
        long timestamp = System.currentTimeMillis();
        while (readRunFlag && ((System.currentTimeMillis() - timestamp) < seconds * 1000)) {
            if (inputStream.available() > 0) {
                int b = inputStream.read();
                log.debug("readByte 读取：" + String.valueOf(b));
                if (b == 67)
                    readRunFlag = false;
            } else {
                ThreadUtils.threadSleep(10);
            }
        }
    }

    public void threadSleep(int millisecond) {
        try {
            Thread.sleep(millisecond);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public interface ObdUpdateCallback {
        public void onResult(boolean updateResult, String obdBinVersionUpdated);
    }

    public void setObdUpdateCallback(ObdUpdateCallback obdUpdateCallback) {
        this.obdUpdateCallback = obdUpdateCallback;
    }
}
