package com.dudu.aios.uvc;

public class Native {

    public static native String stringFromJNI();
    public static native void uvcSetFileName(byte[] filename, int size);
    public static native void uvcSetThumbnailFileName(byte[] filename, int size);
    public static native int uvcAP(String dev);
    public static native void uvcStop();

    static {
        System.loadLibrary("uvc");
    }
}
