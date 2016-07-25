package com.dudu.rest.model;

import com.dudu.rest.model.common.RequestResponse;

/**
 * Created by lxh on 2016-07-04 18:13.
 */
public class VipNavigationResponse extends RequestResponse {

    public VipNavigationPosition result;

    public class VipNavigationPosition {
        public String messageType;
        public String createTime;
        public String position;
        public String lon;
        public String lat;

        public String getMessageType() {
            return messageType;
        }

        public String getCreateTime() {
            return createTime;
        }

        public String getPosition() {
            return position;
        }

        public String getLon() {
            return lon;
        }

        public String getLat() {
            return lat;
        }

        public void setMessageType(String messageType) {

            this.messageType = messageType;
        }

        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }

        public void setPosition(String position) {
            this.position = position;
        }

        public void setLon(String lon) {
            this.lon = lon;
        }

        public void setLat(String lat) {
            this.lat = lat;
        }
    }

}
