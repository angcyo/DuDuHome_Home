package tm.dudu.ext;

import android.util.Log;

import com.dudu.android.hideapi.SystemPropertiesProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by dengjun on 2016/5/17.
 * Description :
 */
public class GpioControl {
    public static final String HIGH = "1";
    public static final String LOW = "0";
    public static final String WAKE_OBD = "/sys/devices/soc.0/obd_gpio.68/obd_wakeup";
    public static final String POWERON_OBD = "/sys/devices/soc.0/obd_gpio.68/obd_power_enable";
//    private static final String POWEROFF_OBD = "/sys/devices/soc.0/obd_gpio.68/obd_power_enable";
    public static final String POWERON_TPMS = "/sys/devices/soc.0/obd_gpio.68/tire_power_enable";
//    private static final String POWEROFF_TPMS = "/sys/devices/soc.0/obd_gpio.68/tire_power_enable";
    public static final String RESET_OBD = "/sys/bus/platform/devices/obd_gpio.68/obd_reset_enable";

    private static Logger log = LoggerFactory.getLogger("car.obd");

    public static void wakeObd() {
        log.info("唤醒obd");
        /*com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
                setCmd(SystemPropertiesProxy.SYS_OBD_WAKE, SystemPropertiesProxy.high);*/

        if(writeDevice(WAKE_OBD, HIGH)) {
            log.info("唤醒obd完成");
        } else {
//        ShellExe.execShellCmd(WAKE_OBD);
            log.info("唤醒obd失败");
        }
    }

    public static void powerOnObd() {
        log.info("obd上电");
/*        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
                setCmd(SystemPropertiesProxy.OBD_POWER, SystemPropertiesProxy.high);*/
//        ShellExe.execShellCmd(POWERON_OBD);
        if(writeDevice(POWERON_OBD, HIGH)) {
            log.info("obd上电完成");
        } else {
            log.info("obd上电失败");
        }
    }

    public static void powerOffObd() {
        log.info("obd下电");
//        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
//                setCmd(SystemPropertiesProxy.OBD_POWER, SystemPropertiesProxy.low);
//        ShellExe.execShellCmd(POWEROFF_OBD);
        if(writeDevice(POWERON_OBD, LOW)) {
            log.info("obd下电完成");
        } else {
            log.info("obd下电失败");
        }
    }

    public static void powerOnTPMS() {
        log.info("TPMS上电");
//        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
//                setCmd(SystemPropertiesProxy.TPMS_POWER, SystemPropertiesProxy.high);
//        ShellExe.execShellCmd(POWERON_TPMS);
        if(writeDevice(POWERON_TPMS, HIGH)) {
            log.info("TPMS上电完成");
        } else {
            log.info("TPMS上电失败");
        }
    }

    public static void powerOffTPMS() {
        log.info("TPMS下电");
//        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
//                setCmd(SystemPropertiesProxy.TPMS_POWER, SystemPropertiesProxy.low);
//        ShellExe.execShellCmd(POWEROFF_TPMS);
//        log.info("TPMS下电完成");
        if(writeDevice(POWERON_TPMS, LOW)) {
            log.info("TPMS下电完成");
        } else {
            log.info("TPMS下电失败");
        }
    }

    public static void wakeObdAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                wakeObd();
            }
        }).start();
    }

    public static void powerOnObdAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                powerOnObd();
            }
        }).start();
    }

    public static void powerOffObdAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                powerOffObd();
            }
        }).start();
    }

    public static boolean writeDevice(String device, String value) {
        try {
            BufferedWriter bufWriter = null;
            bufWriter = new BufferedWriter(new FileWriter(device));
            bufWriter.write(value);  // 写操作
            bufWriter.close();
            log.info("write ok");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            log.info("write error");
            return false;
        }
    }
}
