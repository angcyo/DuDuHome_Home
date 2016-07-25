package com.dudu.workflow.push.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.utils.NetworkUtils;
import com.dudu.rest.common.IpUtils;
import com.dudu.workflow.driving.CarStatusUtils;
import com.dudu.workflow.push.PushService;
import com.dudu.workflow.push.model.UserInfo;
import com.dudu.workflow.push.ping.PingFrameBuilder;
import com.dudu.workflow.push.receiver.SocketService;
import com.dudu.workflow.push.receiver.SocketServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;


/**
 * Created by dengjun on 2016/5/22.
 * Description :
 */
public class WebSocketHandler extends Handler {
    private Logger log = LoggerFactory.getLogger("workFlow.webSocket");

    private WebSocketHandler webSocketHandler = this;
    private SocketService socketService = null;

    private URI uri;

    private PushService pushService;

    private int reConnectDelaySeconds = 10;
    private int heatBeatPeriod = 270;

    public WebSocketHandler(Looper looper, PushService pushService) {
        super(looper);
        this.pushService = pushService;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            SocketHandlerMessage socketHandlerMessage = (SocketHandlerMessage) msg.obj;
            log.info(socketHandlerMessage.getMessage());
            switch (socketHandlerMessage) {
                case DISCONNECT:
                    disConnect();
                    break;
                case RE_CONNECT:
                    if (socketService != null && socketService.isOpen()) {
                        log.debug("SocketService处于连接的状态..");
                        return;
                    }
                    reConnect();
                    break;
                case LONGIN:
                    login();
                    sendMessageDelay(SocketHandlerMessage.LONGIN, heatBeatPeriod);
                    break;
                case HEART_BEAT:
                    socketService.sendPingFrame(new PingFrameBuilder());
                    sendMessageDelay(SocketHandlerMessage.HEART_BEAT, heatBeatPeriod);
                    sendMessageDelay(SocketHandlerMessage.HEART_BEAT_NO_RESPONSE, 15);
                    break;
                case HEART_BEAT_NO_RESPONSE:
                    log.debug("心跳没有响应，将进行立马重连。。");
                    removeMessage(SocketHandlerMessage.HEART_BEAT);
                    reConnect();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("异常", e);
        }
    }

    private void connect() {
        if (socketService == null) {
            socketService = new SocketServiceImpl(uri);
        }
        socketService.setWebSocketCallBack(pushService);
        socketService.openSocketService();
    }

    private void disConnect() {
        if (socketService != null) {
            socketService.closeSocketService();
            socketService = null;
        }
    }

    private void reConnect() {

        disConnect();
        if (NetworkUtils.isNetworkConnected()) {
            connect();
        } else {
            reConnectDelaySeconds = CarStatusUtils.isCarFired() ? 10 : 270;
            log.info("网络不通，{}秒后重连", reConnectDelaySeconds);
            sendMessageDelay(SocketHandlerMessage.RE_CONNECT, reConnectDelaySeconds);
        }
    }


    private void login() {
        if (socketService != null && socketService.isOpen()) {
            log.info("websocket 登录");
            socketService.sendMessage(IpUtils.requestArgsEncrypt(new UserInfo("dudu_websocket", "dudu20150806", CommonLib.getInstance().getObeId(), "mirror")));
            socketService.isOpen();
        } else {
            log.info("websocket 未连接");
        }
    }

    public void sendMessage(SocketHandlerMessage socketHandlerMessage) {
        if (webSocketHandler != null) {
            Message message = webSocketHandler.obtainMessage(socketHandlerMessage.getNum(), socketHandlerMessage);
            webSocketHandler.sendMessage(message);
        }
    }

    public void sendMessageDelay(SocketHandlerMessage socketHandlerMessage, int seconds) {
        if (webSocketHandler != null) {
            Message message = webSocketHandler.obtainMessage(socketHandlerMessage.getNum(), socketHandlerMessage);
            webSocketHandler.sendMessageDelayed(message, seconds * 1000);
        }
    }


    public void removeMessage(SocketHandlerMessage socketHandlerMessage) {
        if (webSocketHandler != null) {
            webSocketHandler.removeMessages(socketHandlerMessage.getNum(), socketHandlerMessage);
        }
    }


    public void removeAllMessages() {
        if (webSocketHandler != null) {
            webSocketHandler.removeAllMessages();
        }
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}
