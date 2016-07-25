package com.dudu.commonlib.utils.shell;

/**
 * Created by dengjun on 2016/6/8.
 * Description :
 */
public class ShellExeTest {
    public static boolean testFlag = false;
    public static void startTest(){
        testFlag = true;
        new Thread(()->{
            while (testFlag){
                ShellExe.execShellCmd("echo 0 > /sys/bus/platform/devices/obd_gpio.68/usb_id_enable");
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(()->{
            while (testFlag){
                ShellExe.execShellCmd("echo 1 > /sys/devices/soc.0/obd_gpio.68/obd_power_enable");
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(()->{
            while (testFlag){
                ShellExe.execShellCmd("echo 1 > /sys/bus/platform/devices/obd_gpio.68/anti_burglary_enable");
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void stopTest(){
        testFlag = false;
    }


    public static void startTest1() {
        testFlag = true;
        new Thread(() -> {
            while (testFlag) {
                ShellExe.execShellCmd("echo 0 > /sys/bus/platform/devices/obd_gpio.68/usb_id_enable");
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> {
            while (testFlag) {
                ShellExe.execShellCmd("echo 1 > /sys/devices/soc.0/obd_gpio.68/obd_power_enable");
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> {
            while (testFlag) {
                ShellExe.execShellCmd("echo 1 > /sys/bus/platform/devices/obd_gpio.68/anti_burglary_enable");
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
