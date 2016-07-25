package com.dudu.monitor.obd.modol;

/**
 * Created by dengjun on 2015/11/26.
 * Description :
 */
public class FlamoutDataUpload {
    private int a;       //热车时长
    private int b;         // 怠速时长(min)

    private float c; //行驶时长(min)
    private float d;        //此次里程(km)

    private float e; //怠速油耗量
    private float f;  // 本次行驶耗油量

    private int g;            //最大发动机转速(rpm)
    private int h;            //最大车速(km/h)

    private int i;   //本次急加速次数 Times
    private int j;   //本次急减速次数 Times

    private String k;    //采集时间

    private String l; //totalDistance;    //总里程
    private String m; //remainL;    //剩余油量

    public FlamoutDataUpload(FlamoutData flamoutData) {
        a = flamoutData.getHotCarTime();
        b = flamoutData.getIdleTime();

        c = flamoutData.getTimes();
        d = flamoutData.getMileT();

        e = flamoutData.getIdleFuel();
        f = flamoutData.getIdleFuelConsumption();

        g = flamoutData.getMaxrpm();
        h = flamoutData.getMaxspd();

        i = flamoutData.getAccNum();
        j = flamoutData.getSharpDownNum();

        k = flamoutData.getCreateTime();

        l = flamoutData.getTotalDistance();
        m = flamoutData.getRemainL();

    }

    /* 用于测试*/
    public FlamoutDataUpload() {
        a = 20;
        b = 150;

        c = 20;
        d = 20;

        e = 20;
        f = 20;

        g = 20;
        h = 20;

        i = 20;
        j = 20;

        k = "20160315";

        l = "60";
        m = "60";

    }

    public String getTotalDistance() {
        return l;
    }

    public void setTotalDistance(String totalDistance) {
        this.l = totalDistance;
    }

    public String getRemainL() {
        return m;
    }

    public void setRemainL(String remainL) {
        this.m = remainL;
    }
}
