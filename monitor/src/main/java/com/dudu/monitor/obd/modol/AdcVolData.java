package com.dudu.monitor.obd.modol;

/**
 * 电瓶ADC电压数据类
 * Created by Robert on 2016/7/7.
 */
public class AdcVolData {

    private String adcVoltage; //电瓶ADC电压
    private String stateFlag; //电压类型， 0：启动最低电压: 1：点火后电压 2：熄火后电压


    public AdcVolData(String adcVoltage, String stateFlag) {
        this.adcVoltage = adcVoltage;
        this.stateFlag = stateFlag;
    }


    /*初始化值测试用，无实际意义*/
    public AdcVolData() {
        this.adcVoltage = "12.4";
        this.stateFlag = "0";
    }

    public String getAdcVoltage() {
        return adcVoltage;
    }

    public void setAdcVoltage(String adcVoltage) {
        this.adcVoltage = adcVoltage;
    }

    public String getStateFlag() {
        return stateFlag;
    }

    public void setStateFlag(String stateFlag) {
        this.stateFlag = stateFlag;
    }
}
