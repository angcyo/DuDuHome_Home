package com.dudu.rest.model;

/**
 * Created by Administrator on 2016/5/21.
 */
public class PushCallBackResponse {

    public String method;

    public String messageId;

    public PushCallBackResponse(String method, String messageId) {
        this.method = method;
        this.messageId = messageId;
    }
}
