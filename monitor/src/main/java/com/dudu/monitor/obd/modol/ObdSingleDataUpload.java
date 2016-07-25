package com.dudu.monitor.obd.modol;

/**
 * Created by Robert on 2016/6/22.
 */
public class ObdSingleDataUpload {
    private String a;       //acc 电压

    public ObdSingleDataUpload(ObdSingleData singleData) {
        a = singleData.getData();
    }

    /* 用于测试*/
    public ObdSingleDataUpload() {
        a = "12.3";
    }

    public String getData() {
        return a;
    }

    public void setData(String data) {
        this.a = data;
    }
}
