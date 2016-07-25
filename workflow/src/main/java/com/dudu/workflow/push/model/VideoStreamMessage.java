package com.dudu.workflow.push.model;

/**
 * Created by robi on 2016-06-24 19:04.
 */
public class VideoStreamMessage {

    /**
     * Command : REGISTER
     * DeviceID : 835415013389618
     * IP : 192.168.88.181
     * Key :
     * Port : 12345
     * Status : 1
     */

    private String Command;
    private String DeviceID;
    private String IP;
    private String Key;
    private int Port;
    private int Status;

    public void setCommand(String Command) {
        this.Command = Command;
    }

    public void setDeviceID(String DeviceID) {
        this.DeviceID = DeviceID;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public void setKey(String Key) {
        this.Key = Key;
    }

    public void setPort(int Port) {
        this.Port = Port;
    }

    public void setStatus(int Status) {
        this.Status = Status;
    }

    public String getCommand() {
        return Command;
    }

    public String getDeviceID() {
        return DeviceID;
    }

    public String getIP() {
        return IP;
    }

    public String getKey() {
        return Key;
    }

    public int getPort() {
        return Port;
    }

    public int getStatus() {
        return Status;
    }
}
