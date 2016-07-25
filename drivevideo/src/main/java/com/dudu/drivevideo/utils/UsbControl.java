package com.dudu.drivevideo.utils;

import com.dudu.android.hideapi.SystemPropertiesProxy;

import org.slf4j.LoggerFactory;

import tm.dudu.ext.GpioControl;

/**
 * Created by dengjun on 2016/2/20.
 * Description :
 */
public class UsbControl {
    public static String TO_HOST_CMD = "/sys/bus/platform/devices/obd_gpio.68/usb_id_enable";
//    public static String TO_CLIENT_CMD = "echo 0 > /sys/bus/platform/devices/obd_gpio.68/usb_id_enable";






    public static boolean usbHostState = false;

    public static boolean isUsbHostState() {
        return usbHostState;
    }


    public static void setToHost() {
/*        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
                setCmd(SystemPropertiesProxy.USB_HOST, SystemPropertiesProxy.high);*/


       /* try {
            Class appconfig = Class.forName("com.dudu.android.launcher.BuildConfig");
            final Field build_type = appconfig.getDeclaredField("BUILD_TYPE");
            if (TextUtils.equals("noUsbHost", (String) build_type.get(null))) {
                //build type , 禁止 usb host
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        LoggerFactory.getLogger("video.reardrivevideo").debug("USB设置成host-------------");
        String setReturnValue = ShellExe.execShellCmd(TO_HOST_CMD);*/

        if (GpioControl.writeDevice(TO_HOST_CMD, GpioControl.HIGH)) {
            LoggerFactory.getLogger("video.reardrivevideo").debug("设置 usb Host状态成功");
        } else {
            LoggerFactory.getLogger("video.reardrivevideo").debug("设置 usb Host 状态失败");
        }

        usbHostState = true;
        LoggerFactory.getLogger("video.reardrivevideo").debug(" usb host状态：{} , USB设置成host-----结果：{},", usbHostState, true);
    }

    public static void setToHostAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                setToHost();
            }
        }).start();
    }

    public static void setToClient() {
/*        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
                setCmd(SystemPropertiesProxy.USB_HOST, SystemPropertiesProxy.low);*/
        if (GpioControl.writeDevice(TO_HOST_CMD, GpioControl.LOW)) {
            LoggerFactory.getLogger("video.reardrivevideo").debug("设置 usb Client状态成功");
        } else {
            LoggerFactory.getLogger("video.reardrivevideo").debug("设置 usb Client 状态失败");
        }
       /* LoggerFactory.getLogger("video.reardrivevideo").debug("USB设置成client-------------");
        String setReturnValue = ShellExe.execShellCmd(TO_CLIENT_CMD);*/
        usbHostState = false;
        LoggerFactory.getLogger("video.reardrivevideo").debug(" usb host状态：{} , USB设置成client-----结果：{},", usbHostState, true);
    }

    public static void setToClientAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                setToClient();
            }
        }).start();
    }
}
