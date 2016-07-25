package com.dudu.persistence.realmmodel.tirepressure;

import com.dudu.commonlib.CommonLib;

import io.realm.RealmObject;

/**
 * Created by Robert on 2016/6/29.
 */
public class TireInfoSetDataRealm extends RealmObject {

    //    @PrimaryKey
    private String obied;

    //轮胎最高温度值
    private String tireHighestTemperatureValue;

    //前轴胎压最低
    private String frontAxleTirePressureRangeLowest;

    //前轴胎压最高
    private String frontAxleTirePressureRangeHighest;

    //后轴胎压最低
    private String rearAxleTirePressureRangeLowest;

    //后轴胎压最高
    private String rearAxleTirePressureRangeHighest;


    public TireInfoSetDataRealm(String obied) {
        this.obied = obied;
        this.tireHighestTemperatureValue = "70";
        this.frontAxleTirePressureRangeLowest = "2.0";
        this.frontAxleTirePressureRangeHighest = "3.0";
        this.rearAxleTirePressureRangeLowest = "2.0";
        this.rearAxleTirePressureRangeHighest = "3.0";
    }

    public TireInfoSetDataRealm(String obied, String tireHighestTemperatureValue, String frontAxleTirePressureRangeLowest, String frontAxleTirePressureRangeHighest, String rearAxleTirePressureRangeLowest, String rearAxleTirePressureRangeHighest) {
        this.obied = obied;
        this.tireHighestTemperatureValue = tireHighestTemperatureValue;
        this.frontAxleTirePressureRangeLowest = frontAxleTirePressureRangeLowest;
        this.frontAxleTirePressureRangeHighest = frontAxleTirePressureRangeHighest;
        this.rearAxleTirePressureRangeLowest = rearAxleTirePressureRangeLowest;
        this.rearAxleTirePressureRangeHighest = rearAxleTirePressureRangeHighest;
    }

    public TireInfoSetDataRealm() {
        this.obied = CommonLib.getInstance().getObeId();
    }

    public String getObied() {
        return obied;
    }

    public void setObied(String obied) {
        this.obied = obied;
    }

    public String getTireHighestTemperatureValue() {
        return tireHighestTemperatureValue;
    }

    public void setTireHighestTemperatureValue(String tireHighestTemperatureValue) {
        this.tireHighestTemperatureValue = tireHighestTemperatureValue;
    }

    public String getFrontAxleTirePressureRangeLowest() {
        return frontAxleTirePressureRangeLowest;
    }

    public void setFrontAxleTirePressureRangeLowest(String frontAxleTirePressureRangeLowest) {
        this.frontAxleTirePressureRangeLowest = frontAxleTirePressureRangeLowest;
    }

    public String getFrontAxleTirePressureRangeHighest() {
        return frontAxleTirePressureRangeHighest;
    }

    public void setFrontAxleTirePressureRangeHighest(String frontAxleTirePressureRangeHighest) {
        this.frontAxleTirePressureRangeHighest = frontAxleTirePressureRangeHighest;
    }

    public String getRearAxleTirePressureRangeLowest() {
        return rearAxleTirePressureRangeLowest;
    }

    public void setRearAxleTirePressureRangeLowest(String rearAxleTirePressureRangeLowest) {
        this.rearAxleTirePressureRangeLowest = rearAxleTirePressureRangeLowest;
    }

    public String getRearAxleTirePressureRangeHighest() {
        return rearAxleTirePressureRangeHighest;
    }

    public void setRearAxleTirePressureRangeHighest(String rearAxleTirePressureRangeHighest) {
        this.rearAxleTirePressureRangeHighest = rearAxleTirePressureRangeHighest;
    }
}
