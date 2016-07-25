package com.dudu.workflow.obd;

/**
 * Created by Robert on 2016/7/6.
 */
public class WaterWarningData {

    private float engCoolant;   //当前发动机冷却液温度
    private boolean isHighFlag; //冷却液温度是否过高

    public WaterWarningData(float engCoolant, boolean isHighFlag) {
        this.engCoolant = engCoolant;
        this.isHighFlag = isHighFlag;
    }

    public WaterWarningData() {
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
