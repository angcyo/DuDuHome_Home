package com.dudu.workflow.obd;

import com.dudu.android.libserial.SerialManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import android_serialport_api.SerialPort;
import rx.Observable;
import rx.ext.CreateObservable;

public class OBDStream implements CreateObservable.ObservableInterface {
    private static OBDStream ourInstance = new OBDStream();
    private static Logger log = LoggerFactory.getLogger("car.obd");

    public static OBDStream getInstance() {
        return ourInstance;
    }

    private boolean closeCMD = false;
    private OutputStream outputStream = null;
    private Observable<String> obdRawData = null;
    private Observable<String> obdRTString = null;
    private Observable<String> obdTTString = null;
    private Observable<String> obdErrorString = null;
    private Observable<String> obdClearFault = null;
    private Observable<String> obdTSPMON = null;
    private Observable<String> obdTSPMOFF = null;
    private Observable<String[]> OBDRTData = null;
    private Observable<Double> testSpeedStream = null;
    private Observable<String[]> OBDTTData = null;
    private Observable<Double> engSpeedStream = null;
    private Observable<Double> speedStream = null;
    private Observable<String> versionStream = null;
    private Observable<String> getCarTypeStream = null;
    private Observable<String> setCarTypeStream = null;
    private Observable<String> batteryVoltageStream = null;
    private Observable<String> accVoltageStream = null;
    private Observable<String> oilRatioStream = null;
    private Observable<String> totalDistanceStream = null;
    private Observable<String> remainLStream = null;
    private Observable<String> startVolStream = null;
    private Observable<String> sleepModeObservable = null;

    private OBDStream() {
    }

    public Observable<String> obdRawData() {
        return obdRawData;
    }

    public void init() {
        log.debug("调用初始化");
        closeCMD = false;
        if (obdRawData == null) {
            log.debug("初始化流");
            SerialPort serialPort = SerialManager.getInstance().getSerialPortOBD();
            InputStream inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            obdRawData = CreateObservable.from(new InputStreamReader(inputStream), this);
        }
    }

    @Override
    public boolean talkWithObservable() {
        if (closeCMD == true) {
            SerialManager.getInstance().closeSerialPortOBD();
            log.debug("关闭OBD流");
            closeCMD = false;
            return true;
        }
        return false;
    }

    public void obdStreamClose() {
        outputStream = null;
        obdRawData = null;
        obdRTString = null;
        obdTTString = null;
        obdErrorString = null;
        obdClearFault = null;
        obdTSPMON = null;
        obdTSPMOFF = null;
        OBDRTData = null;
        testSpeedStream = null;
        OBDTTData = null;
        engSpeedStream = null;
        speedStream = null;
        versionStream = null;
        getCarTypeStream = null;
        setCarTypeStream = null;
        batteryVoltageStream = null;
        accVoltageStream = null;
        oilRatioStream = null;
        totalDistanceStream = null;
        remainLStream = null;
        startVolStream = null;
        sleepModeObservable = null;
        closeCMD = true;

        //SerialManager.getInstance().closeSerialPortOBD();
        //log.debug("关闭OBD流");
    }

    public void exec(String cmd) throws IOException {
        String send = cmd + "\r\n";

        if (outputStream != null) {
            log.debug("执行命令:{}", cmd);
            outputStream.write(send.getBytes(StandardCharsets.US_ASCII));
        } else {
            log.error("执行命令:outputStream为空");
        }
    }

    public Observable<String> obdRTString() throws IOException {
        if (obdRTString == null) obdRTString = obdRTString(obdRawData());
        return obdRTString;
    }

    public Observable<String> obdTTString() throws IOException {
        if (obdTTString == null) obdTTString = obdTTString(obdRawData());
        return obdTTString;
    }

    public Observable<String> obdErrorString() throws IOException {
        if (obdErrorString == null) {
            obdErrorString = obdErrorString(obdRawData());
        }
        return obdErrorString;
    }

    public Observable<String> obdClearFault() throws IOException {
        if (obdClearFault == null) obdClearFault = obdClearFault(obdRawData());
        return obdClearFault;
    }

    public Observable<String> obdTSPMON() throws IOException {
        if (obdTSPMON == null) obdTSPMON = obdTSPMON(obdRawData());
        return obdTSPMON;
    }

    public Observable<String> obdTSPMOFF() throws IOException {
        if (obdTSPMOFF == null) obdTSPMOFF = obdTSPMOFF(obdRawData());
        return obdTSPMOFF;
    }

    public Observable<String> accVoltageStream() {
        if (accVoltageStream == null) accVoltageStream = obdGetVol(obdRawData());
        return accVoltageStream;
    }

    public Observable<String[]> OBDRTData() throws IOException {
        if (OBDRTData == null) OBDRTData = OBDRTData(obdRTString());
        return OBDRTData;
    }

    public Observable<Double> testSpeedStream() throws IOException {
        if (testSpeedStream == null) testSpeedStream = testSpeedStream(obdRTString());
        return testSpeedStream;
    }

    public Observable<String[]> OBDTTData() throws IOException {
        if (OBDTTData == null) OBDTTData = OBDTTData(obdTTString());
        return OBDTTData;
    }

    public Observable<String> OBDVersion() throws IOException {
        if (versionStream == null) versionStream = obdVersion(obdRawData());
        return versionStream;
    }

    public Observable<String> OBDGetCarType() {
        if (getCarTypeStream == null) getCarTypeStream = obdGetCarType(obdRawData());
        return getCarTypeStream;
    }

    public Observable<String> OBDSetCarType() throws IOException {
        if (setCarTypeStream == null) setCarTypeStream = obdSetCarType(obdRawData());
        return setCarTypeStream;
    }

    public Observable<String> batteryVoltageStream() throws IOException {
        if (batteryVoltageStream == null) batteryVoltageStream = batteryVoltageStream(OBDRTData());
        return batteryVoltageStream;
    }

    public Observable<Double> engSpeedStream() throws IOException {
        if (engSpeedStream == null) engSpeedStream = engSpeedStream(OBDRTData());
        return engSpeedStream;
    }

    public Observable<Double> speedStream() throws IOException {
        if (speedStream == null) speedStream = speedStream(OBDRTData());
        return speedStream;
    }

    public Observable<String> oilRatioStream() throws IOException {
        if (oilRatioStream == null) oilRatioStream = fuelOilRatioStream(OBDRTData());
        return oilRatioStream;
    }

    public Observable<String> totalDistanceStream() throws IOException {
        if (totalDistanceStream == null) totalDistanceStream = totalDistanceStream(obdRawData());
        return totalDistanceStream;
    }

    public Observable<String> remainLStream() throws IOException {
        if (remainLStream == null) remainLStream = remainLStream(obdRawData());
        return remainLStream;
    }

    public Observable<String> startVolStream() {
        if (startVolStream == null) startVolStream = startVolStream(obdRawData());
        return startVolStream;
    }

    public Observable<String> sleepModeObservable() {
        if (sleepModeObservable == null) sleepModeObservable = sleepModeObservable(obdRawData());
        return sleepModeObservable;
    }

    private static Observable<String> obdRTString(Observable<String> input) {
        return input
                .filter(s -> s.startsWith("$OBD-RT"));
    }

    private static Observable<String> obdTTString(Observable<String> input) {
        return input
                .filter(s -> s.startsWith("$OBD-TT"));
    }

    private static Observable<String> obdErrorString(Observable<String> input) {
        return input
                .filter(s -> s.startsWith("$4"));
    }

    private static Observable<String> obdClearFault(Observable<String> input) {
        return input;
    }

    private static Observable<String> obdTSPMON(Observable<String> input) {
        return input
                .filter(s -> s.startsWith("$ATTSPMON+OK"));
    }

    private static Observable<String> obdTSPMOFF(Observable<String> input) {
        return input
                .filter(s -> s.startsWith("$ATTSPMOFF+OK"));
    }

    private static Observable<String> obdGetCarType(Observable<String> input) {
        return input
                .filter(s -> s.startsWith("$ATGETVEHICLE"))
                .map(s -> s.split("="))
                .map(strings -> strings[1]);
    }

    private static Observable<String> obdSetCarType(Observable<String> input) {
        return input
                .filter(s -> s.startsWith("$ATSETVEHICLE"))
                .filter(s1 -> s1.endsWith("OK"));
    }

    private static Observable<String> obdVersion(Observable<String> input) {
        return input
                .filter(s -> s.startsWith("$ATGETVER"))
                .map(s1 -> s1.substring(s1.lastIndexOf("=")))
                .map(s2 -> s2.substring(s2.indexOf("V") + 1).trim());
    }

    private static Observable<String> obdGetVol(Observable<String> input) {
        return input
                .filter(s -> s.startsWith("$ATGETVOL"))
                .doOnNext(s1 -> log.debug(s1))
                .filter(s2 -> s2.contains("="))
                .map(s3 -> s3.split("="))
                .map(strings -> strings[1]);
    }

    private static Observable<String[]> OBDRTData(Observable<String> input) {
        return input
                .map(s -> s.split(","))
                .filter(strings -> strings.length >= 16);
    }

    private static Observable<Double> testSpeedStream(Observable<String> input) {
        return input
                .map(s -> s.split(","))
                .filter(strings -> strings.length == 12)
                .map(strings -> strings[4])
                .map(Double::valueOf);
    }

    private static Observable<String[]> OBDTTData(Observable<String> input) {
        return input
                .map(s -> s.split(","));
//                .filter(strings -> strings.length >= 9);
    }

    public static Observable<Double> engSpeedStream(Observable<String[]> input) {
        return input
                .map(strings -> strings[2])
                .map(Double::valueOf);
    }

    private static Observable<String> batteryVoltageStream(Observable<String[]> input) {
        return input
                .map(strings -> strings[1]);
    }

    private static Observable<Double> speedStream(Observable<String[]> input) {
        return input
                .map(strings -> strings[3])
                .map(Double::valueOf);
    }

    private static Observable<String> fuelOilRatioStream(Observable<String[]> input) {
        return input
                .map(strings -> strings[strings.length - 1])
                .filter(theString -> theString.startsWith("-"))
                .map(oilString -> oilString.substring(1));
    }

    private static Observable<String> totalDistanceStream(Observable<String> input) {
        return input
                .filter(strings -> strings.contains("ATGETMIL"))
                .doOnNext(s1 -> log.debug(s1))
                .filter(s2 -> s2.contains("="))
                .map(s -> s.split("="))
                .map(strings1 -> strings1[1]);
    }

    private static Observable<String> remainLStream(Observable<String> input) {
        return input
                .filter(strings -> strings.contains("ATGETFUEL"))
                .doOnNext(s1 -> log.debug(s1))
                .filter(s2 -> s2.contains("="))
                .map(s -> s.split("="))
                .map(strings1 -> strings1[1]);
    }

    private static Observable<String> startVolStream(Observable<String> input) {
        return input
                .filter(strings -> strings.contains("ATGETSTARTVOL"))
                .doOnNext(s1 -> log.debug(s1))
                .filter(s2 -> s2.contains("="))
                .map(s -> s.split("="))
                .map(strings1 -> strings1[1]);
    }

    private static Observable<String> sleepModeObservable(Observable<String> input) {
        return input
                .filter(strings -> strings.contains("sleep mode") || strings.contains("SLEEP+OK"))
                .doOnNext(s1 -> log.debug(s1));
    }
}
