package com.dudu.workflow.tpms;

/**
 * Created by robi on 2016-07-11 18:05.
 */
public class TPMSParamBean {
    /**
     * 温度预警值
     */
    int temperature = 70;
    /**
     * 前轮胎压最高预警值
     */
    float frontPressureHigh = 3.f;
    /**
     * 前轮胎压最低预警值
     */
    float frontPressureLow = 2.f;
    /**
     * 后轮胎压最高预警值
     */
    float backPressureHigh = 3.f;
    /**
     * 后轮胎压最低预警值
     */
    float backPressureLow = 2.f;

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public float getFrontPressureHigh() {
        return frontPressureHigh;
    }

    public void setFrontPressureHigh(float frontPressureHigh) {
        this.frontPressureHigh = frontPressureHigh;
    }

    public float getFrontPressureLow() {
        return frontPressureLow;
    }

    public void setFrontPressureLow(float frontPressureLow) {
        this.frontPressureLow = frontPressureLow;
    }

    public float getBackPressureHigh() {
        return backPressureHigh;
    }

    public void setBackPressureHigh(float backPressureHigh) {
        this.backPressureHigh = backPressureHigh;
    }

    public float getBackPressureLow() {
        return backPressureLow;
    }

    public void setBackPressureLow(float backPressureLow) {
        this.backPressureLow = backPressureLow;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("temp:");
        builder.append(temperature);
        builder.append(" fl:");
        builder.append(frontPressureLow);
        builder.append(" fh:");
        builder.append(frontPressureHigh);
        builder.append(" bl:");
        builder.append(backPressureLow);
        builder.append(" bh:");
        builder.append(backPressureHigh);

        return builder.toString();
    }
}
