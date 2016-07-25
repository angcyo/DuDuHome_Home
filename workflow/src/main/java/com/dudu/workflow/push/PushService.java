package com.dudu.workflow.push;

import android.os.HandlerThread;
import android.text.TextUtils;

import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.utils.DataJsonTranslation;
import com.dudu.commonlib.utils.IPConfig;
import com.dudu.commonlib.utils.process.ProcessUtils;
import com.dudu.commonlib.utils.time.DateTimeUtils;
import com.dudu.workflow.driving.CarStatusUtils;
import com.dudu.workflow.push.handler.SocketHandlerMessage;
import com.dudu.workflow.push.handler.WebSocketHandler;
import com.dudu.workflow.push.model.ReceiverPushData;
import com.dudu.workflow.push.websocket.WebSocketCallBack;

import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import de.greenrobot.event.EventBus;

/**
 * Created by dengjun on 2016/5/10.
 * Description :
 */
public class PushService implements WebSocketCallBack {
    private URI uri;
    private HandlerThread handlerThread;
    private boolean handlerThreadInitFlag = false;

    private WebSocketHandler webSocketHandler;

    private Logger log = LoggerFactory.getLogger("workFlow.webSocket");

    public PushService() {
        log.info("构造PushService");
    }

    private void initHanderThread() {
        handlerThread = new HandlerThread("WebSocket Thread") {
            @Override
            protected void onLooperPrepared() {
                super.onLooperPrepared();
                log.info("handlerThread 初始化完成");
                initAction();
            }
        };
        handlerThread.start();
    }

    private void initAction() {
        try {
            String addr = "ws://" + IPConfig.getInstance().getSOCKET_ADDRESS() + ":" + IPConfig.getInstance().getSOCKET_PORT() + "";
            log.info("初始化 webSocket地址：{}", addr);
            uri = new URI(addr);

            webSocketHandler = new WebSocketHandler(handlerThread.getLooper(), PushService.this);
            webSocketHandler.setUri(uri);

            connect();
        } catch (URISyntaxException e) {
            log.error("异常", e);
        }
    }

    public void init() {
        log.info("当前线程：{}，当前进程：{}", Thread.currentThread().getName(), ProcessUtils.getCurProcessName(CommonLib.getInstance().getContext()));
        synchronized (this) {
            if (handlerThread == null && handlerThreadInitFlag == false) {
                log.info("FrontCameraService初始化");
                initHanderThread();
                handlerThreadInitFlag = true;
            } else {
                log.info("handlerThread 已经初始化了");
            }
        }
    }

    public void release() {
        synchronized (this) {
            if (handlerThread != null && handlerThread.isAlive()) {
                log.info("WebSocket释放");
                removeMessage(SocketHandlerMessage.LONGIN);
                removeMessage(SocketHandlerMessage.HEART_BEAT);
                removeMessage(SocketHandlerMessage.HEART_BEAT_NO_RESPONSE);
                cancerReConnect();
                disConnect();
                handlerThread.quitSafely();
                handlerThread = null;
                webSocketHandler = null;
                handlerThreadInitFlag = false;
            }
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        cancerReConnect();
        login();
    }

    @Override
    public void onMessage(String message) {
        log.info("收到推送消息：{}", message);
        ReceiverPushData receiverPushData = (ReceiverPushData) DataJsonTranslation.jsonToObject(message, ReceiverPushData.class);
        if (overtimePushOvertime(receiverPushData)) {
            return;
        }
        if (receiverPushData.result != null) {
            EventBus.getDefault().post(receiverPushData);
        } else {
            if (receiverPushData.resultCode == 0) {
                log.info("登录成功---");
                removeMessage(SocketHandlerMessage.HEART_BEAT);
                removeMessage(SocketHandlerMessage.HEART_BEAT_NO_RESPONSE);
                removeMessage(SocketHandlerMessage.LONGIN);
                sendPingFrame();
            }
        }
    }

    @Override
    public void onError(Exception ex) {
        log.error("异常：", ex);
        reConnectWebSocket(5);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (remote == true) {
            log.info("连接由于网络断开或者服务器关闭连接，需要重连");
            removeMessage(SocketHandlerMessage.LONGIN);
            removeMessage(SocketHandlerMessage.HEART_BEAT);
            removeMessage(SocketHandlerMessage.HEART_BEAT_NO_RESPONSE);
            reConnectWebSocket(5);
        }
    }

    @Override
    public void onWebsocketPong(Framedata f) {
        removeMessage(SocketHandlerMessage.HEART_BEAT_NO_RESPONSE);
    }

    public void reConnectWebSocket(int seconds) {
        seconds = CarStatusUtils.isCarFired() ? 10 : 270;

        sendMessageDelay(SocketHandlerMessage.RE_CONNECT, seconds);
    }

    private void cancerReConnect() {
        removeMessage(SocketHandlerMessage.RE_CONNECT);
    }

    private void connect() {
        sendMessage(SocketHandlerMessage.RE_CONNECT);
    }

    private void disConnect() {
        sendMessage(SocketHandlerMessage.DISCONNECT);
    }


    private void login() {
        removeMessage(SocketHandlerMessage.LONGIN);
        if (!webSocketHandler.hasMessages(SocketHandlerMessage.LONGIN.getNum())) {
            sendMessage(SocketHandlerMessage.LONGIN);
        } else {
            log.info("已经有登录消息了");
        }
    }

    private void sendPingFrame() {

        log.debug("30秒后发送心跳的消息");
        if (!webSocketHandler.hasMessages(SocketHandlerMessage.HEART_BEAT.getNum())) {
            sendMessageDelay(SocketHandlerMessage.HEART_BEAT, 30);
        } else {
            log.info("已经有心跳消息了");
        }
    }


    private void sendMessage(SocketHandlerMessage socketHandlerMessage) {
        if (webSocketHandler != null && handlerThread != null && handlerThread.isAlive()) {
            webSocketHandler.sendMessage(socketHandlerMessage);
        }
    }

    private void sendMessageDelay(SocketHandlerMessage socketHandlerMessage, int seconds) {
        if (webSocketHandler != null && handlerThread != null && handlerThread.isAlive()) {
            webSocketHandler.sendMessageDelay(socketHandlerMessage, seconds);
        }
    }


    private void removeMessage(SocketHandlerMessage socketHandlerMessage) {
        if (webSocketHandler != null && handlerThread != null && handlerThread.isAlive()) {
            webSocketHandler.removeMessage(socketHandlerMessage);
        }
    }


    private boolean overtimePushOvertime(ReceiverPushData data) {
        boolean overtime = false;
        if (data != null && data.resultCode == 0 && data.result != null) {

            String messageId = data.result.messageId;
            String requestStartTime = data.result.requestStartTime;
            String liveTime = data.result.livetime;

            //其他推送method, 不需要时间判断.
            if (TextUtils.isEmpty(requestStartTime) || TextUtils.isEmpty(liveTime)) {
                return overtime;
            }

            log.debug("messageId:" + messageId + "  requestStartTime:" + requestStartTime + "   liveTime:" + liveTime);
            Date requestStartDate = DateTimeUtils.transformDate(requestStartTime);
            Date currentDate = new Date();
            log.debug("本地的时间:" + currentDate.toString());
            if (data != null) {
                long diffDate = currentDate.getTime() - requestStartDate.getTime();
                log.debug("与服务器的时间差为" + diffDate + "  ");
                if (diffDate / 1000 > Double.parseDouble(liveTime)) {
                    log.debug("服务器超时,不做任何事。。");
                    overtime = true;
                }
            }
        }
        return overtime;
    }


}
