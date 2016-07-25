package com.dudu.workflow.tpms;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import rx.Observable;

public class TPMSFlow {
    public static Logger logger = LoggerFactory.getLogger("TPMS.checkFlow");

    public static Observable<TPMSInfo> TPMSParse(final byte mainFn) {
        if (TpmsStream.getInstance().rawData() == null) {
            return null;
        }
        return TpmsStream.getInstance().rawData()
                .filter(byteBuffer -> byteBuffer.get(0) == mainFn)
                .filter(byteBuffer -> {
                    int sz = byteBuffer.array().length;
                    logger.debug("byteBuffer size:{}", sz);
                    return sz == 10 | sz == 11;
                })
                .map(byteBuffer -> ByteBuffer.wrap(byteBuffer.array()))
                .map(byteBuffer -> {
                    logger.debug("TPMSFlow: {}", String.copyValueOf(Hex.encodeHex(byteBuffer.array())));
                    byte fn = byteBuffer.get();
                    if (fn != mainFn) throw new RuntimeException("胎压数据传输");
                    TPMSInfo warnInfo = new TPMSInfo();
                    warnInfo.position = byteBuffer.get();
                    if (warnInfo.position == 0) {
                        warnInfo.position = byteBuffer.get();
                    }
                    byteBuffer.get(); //pass one byte
                    warnInfo.sensorID = byteBuffer.getShort();
                    warnInfo.pressure = (float) ((0x03ff & byteBuffer.getShort()) * 0.025);
                    warnInfo.temperature = (0x0ff & byteBuffer.get()) - 50;

                    byte state = byteBuffer.get();
                    warnInfo.battery = (0x80 & state) != 0;
                    warnInfo.noData = (0x40 & state) != 0;
                    warnInfo.barometerHigh = (0x10 & state) != 0;
                    warnInfo.barometerLow = (0x08 & state) != 0;
                    warnInfo.temperatureHigh = (0x04 & state) != 0;
                    warnInfo.gasLeaks = 0x03 & state;
                    return warnInfo;
                })
                .doOnError(throwable -> logger.error("TPMSFlow: TPMSParse :{}", throwable));
    }


    public static Observable<TPMSParamBean> TPMSParseParam(final byte mainFn) {
        if (TpmsStream.getInstance().rawData() == null) {
            return null;
        }
        return TpmsStream.getInstance().rawData()
                .filter(byteBuffer -> byteBuffer.get(0) == mainFn)
                .filter(byteBuffer -> {
                    int sz = byteBuffer.array().length;
                    logger.debug("parse param  byteBuffer size:{}", sz);
                    return sz == 14;
                })
                .map(byteBuffer -> ByteBuffer.wrap(byteBuffer.array()))
                .map(byteBuffer -> {
                    logger.debug("parse param TPMSFlow: {}", String.copyValueOf(Hex.encodeHex(byteBuffer.array())));
                    byte fn = byteBuffer.get();
                    if (fn != mainFn) throw new RuntimeException("胎压预警值数据传输");
                    TPMSParamBean paramBean = new TPMSParamBean();
                    paramBean.setFrontPressureHigh(byteBuffer.get(2) / 10f);
                    paramBean.setFrontPressureLow(byteBuffer.get(3) / 10f);
                    paramBean.setBackPressureHigh(byteBuffer.get(4) / 10f);
                    paramBean.setBackPressureLow(byteBuffer.get(5) / 10f);
                    paramBean.setTemperature(byteBuffer.get(12) - 50);
                    return paramBean;
                })
                .doOnError(throwable -> logger.error("TPMSFlow: TPMSParseParam :{}", throwable));
    }

    public static Observable<TPMSInfo> TPMSWarnInfoStream() {
        final byte mainFn = 0x63;
        if (TPMSParse(mainFn) == null) {
            return null;
        }
        return TPMSParse(mainFn);
    }

    public static Observable<TPMSInfo> TPMSPairStream() {
        final byte mainFn = 0x66;
        if (TPMSParse(mainFn) == null) {
            return null;
        }
        return TPMSParse(mainFn)
//                .filter(tpmsWarnInfo -> tpmsWarnInfo.gasLeaks == 3)
//                .map(tpmsWarnInfo -> TPMSInfo.POSITION.valueOf(tpmsWarnInfo.position))
//                .filter(position -> position != TPMSInfo.POSITION.UNKNOW);
                .filter(tpmsWarnInfo -> TPMSInfo.POSITION.valueOf(tpmsWarnInfo.position) != TPMSInfo.POSITION.UNKNOW);
    }

    public static Observable<TPMSParamBean> TPMSGetParamStream() {
        final byte mainFn = 0x62;
        if (TPMSParse(mainFn) == null) {
            return null;
        }
        return TPMSParseParam(mainFn);
    }

    public static void TPMSPairStart(TPMSInfo.POSITION position) {
        byte[] cmd = {(byte) 0xAA, 0x41, (byte) 0xA1, 0x07, 0x66, 0x00};
        cmd[5] = (byte) position.value();
        TpmsStream.getInstance().write(cmd);
    }

    /**
     * 设置温度报警
     * 数据字节只有1个字节（温度）
     * <p>
     * 温度单位默认是℃，例如：温度=0x78，转换成十进制为120，则报警参数设置为120-50=70℃。
     *
     * @param celsius 摄氏度
     */
    public static void TPMSSetTempParam(int celsius) {
        byte[] cmd = {(byte) 0xAA, 0x41, (byte) 0xA1, 0x08, 0x62, 0x06, (byte) 0x8C};
        cmd[6] = (byte) (celsius + 50);
        TpmsStream.getInstance().write(cmd);
    }

    /**
     * 设置压力报警
     * 子功能号：01H 第一轴（或小车前轮）压力报警参数传输
     * 子功能号：02H 第二轴（或小车后轮）压力报警参数传输
     * 数据字节有2个字节第1字节为高压报警参数，第2字节为低压报警参数
     * <p>
     * 压力单位默认是Bar 例如：设置的压力=0x82，转换成十进制为130，表示压力设置为13.0Bar。
     *
     * @param shaft         小车, 1 表示第一轴, 2表示 第二轴
     * @param barometerHigh 高压报警数值
     * @param barometerLow  低压报警数值
     */
    public static void TPMSSetBarParam(int shaft, int barometerHigh, int barometerLow) {
        byte[] cmd = {(byte) 0xAA, 0x41, (byte) 0xA1, 0x09, 0x62, 0x01, (byte) 0x82, (byte) 0xA2};
        if (shaft == 2) {
            cmd[5] = 0x02;
        }
        cmd[6] = (byte) barometerHigh;
        cmd[7] = (byte) barometerLow;
        TpmsStream.getInstance().write(cmd);
    }

    /**
     * 发送指令
     * <p>
     * 获取TPMS 模块所有的报警参数
     */
    public static void TPMSGetTempParam() {
        byte[] cmd = {(byte) 0xAA, 0x41, (byte) 0xA1, 0x07, 0x62, 0x00};
        TpmsStream.getInstance().write(cmd);
    }

}
