package com.dudu.monitor.obd.modol;

/**
 * Obd单个字符串数据类
 * Created by Robert on 2016/6/22.
 */
public class ObdSingleData {

    private String extData;    //acc电压


    public ObdSingleData(String data) {

        extData = data;
    }

    public String getData() {
        return this.extData;
    }

    public void setData(String data) {
        this.extData = data;
    }

}
