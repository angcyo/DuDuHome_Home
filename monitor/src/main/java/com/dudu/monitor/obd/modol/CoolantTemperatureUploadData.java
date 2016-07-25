package com.dudu.monitor.obd.modol;

/**
 * Created by Robert on 2016/7/4.
 */
public class CoolantTemperatureUploadData {

    private float a;   //当前发动机冷却液温度
    private boolean b; //冷却液温度是否过高


    public CoolantTemperatureUploadData() {
        this.a = 87;
        this.b = false;
    }

    public CoolantTemperatureUploadData(float a, boolean b) {
        this.a = a;
        this.b = b;
    }

    public float getA() {
        return a;
    }

    public void setA(float a) {
        this.a = a;
    }

    public boolean isB() {
        return b;
    }

    public void setB(boolean b) {
        this.b = b;
    }
}
