package com.dudu.workflow.push.handler;

/**
 * Created by dengjun on 2016/5/18.
 * Description :
 */
public enum SocketHandlerMessage {
    CONNECT(0, "连接webSocket服务器"),
    DISCONNECT(1, "断开连接webSocket服务器"),
    RE_CONNECT(2, "重新连接webSocket服务器"),
    LONGIN(3, "登录webSocket服务器"),
    HEART_BEAT(4, "发送心跳"),
    HEART_BEAT_NO_RESPONSE(5, "发送心跳没有15秒后回调");


    public int num;
    private String message;

    SocketHandlerMessage(int num, String message) {
        this.num = num;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
