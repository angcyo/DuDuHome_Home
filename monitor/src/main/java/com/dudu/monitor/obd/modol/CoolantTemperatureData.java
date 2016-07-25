package com.dudu.monitor.obd.modol;

/**
 *
 *  监测水温数据类
 * Created by Robert on 2016/7/4.
 */
public class CoolantTemperatureData {

    private float engCoolant;   //当前发动机冷却液温度
    private boolean isHighFlag; //冷却液温度是否过高

    public CoolantTemperatureData(float engCoolant, boolean isHighFlag) {
        this.engCoolant = engCoolant;
        this.isHighFlag = isHighFlag;
    }

    public CoolantTemperatureData() {
        this.engCoolant = 87;
        this.isHighFlag = false;
    }

    public float getEngCoolant() {
        return engCoolant;
    }

    public void setEngCoolant(float engCoolant) {
        this.engCoolant = engCoolant;
    }

    public boolean isHighFlag() {
        return isHighFlag;
    }

    public void setHighFlag(boolean highFlag) {
        isHighFlag = highFlag;
    }
}
