package me.lake.librestreaming.model;

/**
 * Created by lake on 16-3-18.
 */
public class RESVideoBuff {
    public boolean isReadyToFill;
    public int colorFormat = -1;
    public byte[] buff;

    public RESVideoBuff(int colorFormat, int size) {
        isReadyToFill = true;
        this.colorFormat = colorFormat;
        buff = new byte[size];
    }
}
