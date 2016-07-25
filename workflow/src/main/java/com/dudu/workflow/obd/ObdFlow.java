package com.dudu.workflow.obd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by Administrator on 2016/5/23.
 */
public class ObdFlow {

    private static Logger log = LoggerFactory.getLogger("car.obd");

    public static void setCarTypeAndGetResult(long carType) {
        log.debug("setCarType:" + carType);
        try {
            OBDStream.getInstance().OBDSetCarType()
                    .subscribe(
                            result -> {
                                log.debug("setCarType:success");
                            },
                            throwable -> {
                                log.error("setCarType", throwable);
                            });
            OBDStream.getInstance().exec("ATSETVEHICLE=" + carType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setCarType(long carType) {
        try {
            OBDStream.getInstance().exec("ATSETVEHICLE=" + carType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getCarType() {
        try {
            OBDStream.getInstance().exec("ATGETVEHICLE");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
