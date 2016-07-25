package com.dudu.workflow.obd;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import com.dudu.android.libble.BleConnectMain;
import com.dudu.workflow.common.DataFlowFactory;

import org.scf4a.ConnSession;
import org.scf4a.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.event.EventBus;
import rx.functions.Action1;
import tm.dudu.ext.GpioControl;

public class CarLock {
    private static Logger log = LoggerFactory.getLogger("car.CarLock");
    private static CarLock ourInstance = new CarLock();

    public static CarLock getInstance() {
        return ourInstance;
    }

    public CarLock() {
    }

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mWriteChara;
    private BluetoothDevice mBluetoothDevice;

    @Deprecated
    public void init(Context context) {
        ConnSession.getInstance();
        BleConnectMain.getInstance().init(context);
        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new Event.StartScanner(Event.ConnectType.BLE));
    }

    public void uninit(Context context) {
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(Event.BackScanResult event) {
        if (event.getType() != Event.ConnectType.BLE)
            return;
        BluetoothDevice device = event.getDevice();
        log.debug("ble try Connect {}[{}]", device.getName(), device.getAddress());
        EventBus.getDefault().post(new Event.Connect(device.getAddress(), Event.ConnectType.BLE, false));
    }


    public void onEvent(Event.BLEInit event) {
        log.debug("ble BLEInit");
        mBluetoothGatt = event.getBluetoothGatt();
        mWriteChara = event.getWriteChara();

        mBluetoothDevice = event.getDevice();
        final String devAddr = mBluetoothDevice.getAddress();
    }

    public static final String LOCK_CAR = "/sys/bus/platform/devices/obd_gpio.68/anti_burglary_enable";
//    public static final String UNLOCK_CAR = "/sys/bus/platform/devices/obd_gpio.68/anti_burglary_enable";

    public static void lockCar() {
        log.debug("lockCar");
//        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
//                setCmd(SystemPropertiesProxy.CAR_LOCK, SystemPropertiesProxy.high);
//        ShellExe.execShellCmd(LOCK_CAR);
        if (GpioControl.writeDevice(LOCK_CAR, "1")) {
            log.debug("lockcar 成功");
        } else {
            log.debug("lockcar 失败");
        }
    }

    public static void unlockCar() {
        log.debug("unlockCar");
//        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
//                setCmd(SystemPropertiesProxy.CAR_LOCK, SystemPropertiesProxy.low);
//        ShellExe.execShellCmd(UNLOCK_CAR);
        if (GpioControl.writeDevice(LOCK_CAR, "0")) {
            log.debug("unlockCar 成功");
        } else {
            log.debug("unlockCar 失败");
        }
    }

    public static void guardLockCar() {
        log.debug("上锁");
        lockCar();
    }

    public static void guardUnlockCar() {
        DataFlowFactory.getRobberyMessageFlow().getRobberyTriggerSwitch()
                .filter(isRobberTrigger->!isRobberTrigger)
                .doOnNext(isRobberyTrigger1->log.debug("开始解锁"))
                .subscribe(robberyTfiggerSwitch -> unlockCar(), throwable -> log.error("guardUnlockCar",throwable));
    }

    public static void robberyLockCar() {
        log.debug("上锁");
//        ObservableFactory.setRobberyTriggered(true);
        lockCar();
    }

    public static void robbertUnlockCar() {
//        ObservableFactory.setRobberyTriggered(false);
        DataFlowFactory.getSwitchDataFlow().getGuardSwitch().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                if (!aBoolean) {
                    log.debug("开始解锁。。");
                    unlockCar();
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                log.debug("throwable:" + throwable);
            }
        });
    }
}
