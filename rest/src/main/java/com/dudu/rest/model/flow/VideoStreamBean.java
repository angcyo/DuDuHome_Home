package com.dudu.rest.model.flow;

import com.dudu.rest.model.common.RequestResponse;

/**
 * Created by robi on 2016-06-22 18:22.
 */
public class VideoStreamBean extends RequestResponse {
    public Command result;

    public class Command {
        /**
         * Command : STREAM
         * DeviceID : 865415013354679
         * IP : 192.168.0.1
         * Status : 12
         * Port:0
         */

        private String Command;
        private String DeviceID;
        private String IP;
        private int Status;
        private int Port;

        public int getPort() {
            return Port;
        }

        public void setPort(int port) {
            Port = port;
        }

        public String getCommand() {
            return Command;
        }

        public void setCommand(String Command) {
            this.Command = Command;
        }

        public String getDeviceID() {
            return DeviceID;
        }

        public void setDeviceID(String DeviceID) {
            this.DeviceID = DeviceID;
        }

        public String getIP() {
            return IP;
        }

        public void setIP(String IP) {
            this.IP = IP;
        }

        public int getStatus() {
            return Status;
        }

        public void setStatus(int Status) {
            this.Status = Status;
        }

        //{"Command":"STREAM","DeviceID":"865415013354679","IP":"192.168.0.1","Status":12}
    }
}
