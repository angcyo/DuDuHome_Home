package com.dudu.network.event;

import com.dudu.network.d01code.message.MessagePackage;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dengjun on 2015/11/30.
 * Description : 4.2.2 流量超限预警
 */
public class DataOverstepAlarm extends MessagePackage {
    private String messageId;
    private  int alarmLevel ;
    private  String method;

    @Override
    public void setMessageId(String messageId) {

    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public boolean isNeedWaitResponse() {
        return false;
    }

    @Override
    public boolean isNeedEncrypt() {
        return false;
    }

    @Override
    public void createFromJsonString(String messageJsonString) {
        try {
            JSONObject jsonObject = new JSONObject(messageJsonString);
            messageId = jsonObject.getString("messageId");
            alarmLevel =  jsonObject.getInt("alarmLevel");
            method = jsonObject.getString("method");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toJsonString() {
        return null;
    }

    @Override
    public boolean isNeedCache() {
        return false;
    }

    public int getAlarmLevel() {
        return alarmLevel;
    }
}
