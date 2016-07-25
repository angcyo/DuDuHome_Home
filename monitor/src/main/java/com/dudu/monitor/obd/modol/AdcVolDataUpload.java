package com.dudu.monitor.obd.modol;

/**
 * 电瓶ADC电压上传数据类
 * (a,b仅仅是为了节省上传数据长度)
 * Created by Robert on 2016/7/7.
 */
public class AdcVolDataUpload {

    private String a; //电瓶ADC电压
    private String b; //电压类型， 0：启动最低电压: 1：点火后电压 2：熄火后电压


    public AdcVolDataUpload() {

    }

    public AdcVolDataUpload(String a, String b) {
        this.a = a;
        this.b = b;
    }


    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }
}
