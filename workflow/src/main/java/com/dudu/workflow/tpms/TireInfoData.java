package com.dudu.workflow.tpms;

/**
 * 胎压参数上传参数类
 * Created by Robert on 2016/6/29.
 */
public class TireInfoData {

    private int postion;   //轮胎位置
    private int sensorID;  //传感器ID
    private float pressure; //压力 Bar
    private int temperature; //温度 摄氏度
    private int gasLeaks; //0:正常, 1:急漏气, 2:慢漏气, 3:加气
    private boolean battery; //0:正常, 1:低压
    private boolean noData;  //0:正常, 1:30分钟未收到发射器到数据
    private boolean barometerHigh; //气压高
    private boolean barometerLow;  //气压低
    private boolean temperatureHigh; //0:正常, 1:温度高

    public TireInfoData(TirePressureData tirePressureData) {
        this.postion = tirePressureData.getPostion();
        this.pressure = tirePressureData.getPressure();
        this.temperature = tirePressureData.getTemperature();
        this.barometerHigh = tirePressureData.isBarometerHigh();
        this.barometerLow = tirePressureData.isBarometerLow();
        this.temperatureHigh = tirePressureData.isTemperatureHigh();
        this.sensorID = tirePressureData.getSensorID();
        this.battery = tirePressureData.isBattery();
        this.noData = tirePressureData.isNoData();
        this.gasLeaks = tirePressureData.getGasLeaks();
    }
}
