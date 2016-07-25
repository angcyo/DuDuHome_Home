package com.dudu.drivevideo.rearcamera.utils;

import com.dudu.commonlib.utils.File.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by dengjun on 2016/6/23.
 * Description :
 */
public class DeviceUtils {
    private static Logger log = LoggerFactory.getLogger("video.reardrivevideo");

    public static boolean detectCamera(String videoName) {
        return FileUtil.detectFileExist("/dev/" + videoName);
    }

    public static void printDevVideos() {
        File file = new File("/dev");
        if (file.exists()) {
            File[] files = file.listFiles();
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("video设备文件：");
            for (File file1 : files) {
                if (file1.getName().startsWith("video")) {
                    stringBuffer.append(file1.getName() + ",");
                }
            }
            log.info(stringBuffer.toString());
        }
    }
}
