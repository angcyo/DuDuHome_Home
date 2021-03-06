package com.dudu.network.d01code;

import com.dudu.commonlib.CommonLib;
import com.dudu.network.client.mina.MinaConnection;
import com.dudu.network.event.Login;
import com.dudu.network.d01code.interfaces.IConnectCallBack;
import com.dudu.network.d01code.interfaces.IConnection;

import com.dudu.network.client.netty.server.ObdServerBootstrap;
import com.dudu.network.utils.DuduLog;
import com.dudu.network.utils.Encrypt;
import com.dudu.network.valueobject.ConnectionParam;
import com.dudu.network.valueobject.ConnectionState;
import com.dudu.network.d01code.message.MessagePackage;

import com.dudu.obd.common.LoginMsg;
import com.dudu.storage.Storage;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by dengjun on 2015/11/28.
 * Description :  网络服务，处理消息发送，消息接收，网络不通时保存需要保存的消息
 */
public class NetworkService implements IConnectCallBack {
    //阻塞队列，用于存放要发送的消息
    private BlockingQueue<MessagePackage> messagePackagesQueue;

    private IConnection iConnection = null;
    private ConnectionParam connectionParam;

    private Logger log;
    private int log_step = 0;

    //发送数据线程运行标记
    private boolean sendThreadRunFlag = false;
    //发送数据后，上锁， 发送数据成功后释放锁
    private String sendMessageLock = "SendMessageLock";
    //当前发送的消息包
    private MessagePackage curSendMessagePackage;
    //当前发送无需响应的消息的消息
    private String curNoNeedResponseMessageString;

    private ScheduledExecutorService sendThreadPool = null;

    //消息处理器，对服务器发来的消息处理
    private MessageHandler messageHandler;
    //此类处理发送失败保存在文件的中的消息，发送失败的消息并不是所有的都需要保存，见协议文档
    private StorageMessageHandler storageMessageHandler;

    //存储数据的时候对队列上锁
    private String stotageMessageLock = "StotageMessageLock";

    /* 登录状态，设备发送其他消息前，需要先登录*/
    private boolean isLogined = false;



    public NetworkService() {
        iConnection = new MinaConnection();


        iConnection.setConnectCallBack(this);
        messagePackagesQueue = new ArrayBlockingQueue<MessagePackage>(600, true);

        messageHandler = new MessageHandler(this);
        storageMessageHandler = new StorageMessageHandler(this);

        log = LoggerFactory.getLogger("network");


    }

    private Thread sendThread = new Thread() {
        @Override
        public void run() {
            while (sendThreadRunFlag) {
                try {
                    if (CommonLib.getInstance().getVersionManage().isDemoVersionFlag()){
                        if (isLogined()) {
                            sendMessage();
                        }else {
                            sendThreadWait();
                        }
                    }else {
                        if (iConnection.isConnected()) {
                            sendMessage();
                        }else {
                            sendThreadWait();
                        }
                    }

                    checkAndStorageMessage();//检查并处理消息是否需要持久化
                } catch (Exception e) {
                    log.error("异常:", e);
                    e.printStackTrace();
                }
            }
        }
    };

    /* 发送线程等待*/
    private void sendThreadWait() throws InterruptedException{
        synchronized (sendThread) {
            log.debug("----发送线程等待---：");
            sendThread.wait(30 * 1000);
        }
    }

    private void sendMessage(){
        MessagePackage messagePackageToSend = nextMessagePackage();
        sendMessageReal(messagePackageToSend);
        if (messagePackageToSend.isNeedWaitResponse())
            waitResponse();//等待响应
    }

    //检查是否需要对消息队列的数据进行持久化处理，如果大小大于200条就进行持久化
    private void checkAndStorageMessage() {
        synchronized (stotageMessageLock) {
            if (messagePackagesQueue.size() > 50) {
                log.info("消息持久化----messagePackagesQueue.size()：" + messagePackagesQueue.size());
                for (int i = 0; i < messagePackagesQueue.size(); i++) {
                    MessagePackage messagePackage = messagePackagesQueue.remove();
                    String messageToStorage = messagePackage.toJsonString();
//                    log.debug("存储消息：{}", messageToStorage);

                    if (messagePackage.isNeedCache()) { //把消息的method字段和消息拼接起来保存到文件
                        Storage.getInstance().saveData(messagePackage.getMethod() + "@" + messageToStorage);
                    }
                }
                Storage.getInstance().flush();//无需刷新
                log.info("消息持久化----完成");
            }
        }
    }

    //发送数据异步发送，消息放到阻塞队列，由发送线程统一发送
    public void sendMessage(MessagePackage messagePackage) {
//        synchronized (stotageMessageLock){//暂不加锁
        try {
            log.debug("发送消息：{}，messagePackagesQueue消息队列大小：{}, 消息ID：{}" , log_step++,messagePackagesQueue.size(), messagePackage.getMessageId());
            messagePackagesQueue.put(messagePackage);
        } catch (InterruptedException e) {
            log.error("异常:",e);
        }
        synchronized (messagePackagesQueue) {
            messagePackagesQueue.notifyAll();
        }
    }

    private MessagePackage nextMessagePackage() {
        MessagePackage messagePackage = null;
        while ((messagePackage = messagePackagesQueue.peek()) == null) {//只取，不删，发送成功才删除
            try {//为null的情况说明队列里面没有要发送的消息，等待有发送的消息
                synchronized (messagePackagesQueue) {
                    messagePackagesQueue.wait();
                }
            } catch (InterruptedException e) {
                log.error("获取下一条消息异常:",e);
            }
        }
        return messagePackage;
    }

    private void waitResponse() {
        try {//是否需要等待，后续待定
            synchronized (sendMessageLock) {
                log.debug("----发送消息后--等待响应---");
                sendMessageLock.wait(30 * 1000);//后续做时间控制,
            }
        } catch (InterruptedException e) {
            log.error("异常:" ,e);
        }
    }

    private void sendMessageReal(MessagePackage messagePackage) {
        curSendMessagePackage = messagePackage;
        String sendMessage = messagePackage.toJsonString();
        log.info("发送消息_加密前 messageID：" + messagePackage.getMessageId() + "  消息内容：" + sendMessage);
        try {
            if (messagePackage.isNeedEncrypt()) {
                sendMessage = Encrypt.AESEncrypt(sendMessage, Encrypt.vi);
//                log.debug("发送消息_加密后 messageID：" + messagePackage.getMessageId() + "  消息内容：" + sendMessage);
            } else {
                log.info("响应服务器发送的消息-----------");
                curNoNeedResponseMessageString = sendMessage;
            }

            iConnection.sendMessage(sendMessage);
        } catch (Exception e) {
            log.error("异常:", e);
        }
    }


    //初始化网络服务
    public void init(ConnectionParam connectionParam) {
        this.connectionParam = connectionParam;

        sendThreadRunFlag = true;
//        sendThreadPool = Executors.newScheduledThreadPool(2);
//        sendThreadPool.execute(sendThread);
//        startNettyServer();
//        sendThreadPool.scheduleAtFixedRate(keepAliveThread, 5, 40, TimeUnit.SECONDS);
//        startNettyClient();
//        iConnection.connect(this.connectionParam);
//        messageHandler.init();
    }

    //结束网络服务
    public void release() {
        sendThreadRunFlag = false;

        //如果队列还有数据未发送完，需要等待或者持久化


        if (sendThreadPool != null && !sendThreadPool.isShutdown()) {
            try {
                sendThreadPool.shutdown();
            } catch (Exception e) {
                log.error("异常:" + e);
            }
            sendThreadPool = null;
        }
        iConnection.disConnect();
        messageHandler.release();
    }

    public void connect(ConnectionParam connectionParam) {
        iConnection.connect(connectionParam);
    }

    @Override
    public void onConnectionState(ConnectionState connectionState) {
        log.info("网络状态：" + connectionState.connectionState);
        switch (connectionState.connectionState) {
            case ConnectionState.CONNECTION_CREATE:
                break;
            case ConnectionState.CONNECTION_FAIL://当连接被关闭的时候，此方法被调用。
            case ConnectionState.CONNECTION_IDLE://默认情况不会有限制状态
                if (CommonLib.getInstance().getVersionManage().isDemoVersionFlag()){
                    setIsLogined(false);
                }
                break;
            case ConnectionState.CONNECTION_SUCCESS:
                if (CommonLib.getInstance().getVersionManage().isDemoVersionFlag()){
                    sendLoginMessage();
                }else {
                    notifySendThread();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onReceive(String messageReceived) {
        try {
            messageHandler.processReceivedMessage(new JSONObject(messageReceived));
        } catch (JSONException e) {
            log.error("异常:",e);
        }
    }

    /*发送登录消息 */
    public void sendLoginMessage(){
        curSendMessagePackage = new Login();
        sendMessageReal(curSendMessagePackage);
    }

    /* 通知发送线程可以发送消息了*/
    public void notifySendThread(){
        synchronized (sendThread) {
            log.debug("通知发送线程，可以发送消息了--------");
            sendThread.notify();
        }
    }

    public void removeHeadOfMessageQueue() {
        if (messagePackagesQueue.size()>=1){
            MessagePackage messagePackage = messagePackagesQueue.peek();
            if (messagePackage.equals(curSendMessagePackage)){
                log.debug("删除队列头----------");
                messagePackagesQueue.remove();
            }
        }
    }

    //通知可以发送下一条
    public void nodifyReceiveResponse() {
        synchronized (sendMessageLock) {
            log.debug("发送数据收到响应，通知可以发送下一条--------");
            sendMessageLock.notifyAll();//通知可以发送下一条数据了
        }
    }

    public MessagePackage getCurSendMessagePackage() {
        return curSendMessagePackage;
    }

    @Override
    public void onMessageSent(String messageSent) {//执行这里时，表明数据发送成功了
//        sendMessageLock.notify();//通知可以发送下一条数据了
        if (messageSent.equals(curNoNeedResponseMessageString)) {
            log.debug("无需响应的消息，发送成功后就删除-------");
            removeHeadOfMessageQueue();
        }
    }

    private Thread keepAliveThread = new Thread() {
        @Override
        public void run() {
            if (sendThreadRunFlag == false)//发送线程无需运行的时候，说明设备要休眠了
                return;
            try {
                if (iConnection.isConnected() == false) {
                    iConnection.disConnect();
                    log.info("守护线程重连网络---");
                    iConnection.connect(connectionParam);
                }
                if (iConnection.isConnected()){//网络连接正常的情况下才去做处理持久化了的数据
                    storageMessageHandler.proStorageMessage();
                }
            } catch (Exception e) {
                log.error("异常:",e);
            }
        }
    };

    private void startNettyServer(){
        log.info("开启netty   server---");
        new Thread() {
            @Override
            public void run() {
                try {
                    log.info("开启netty   server-----------");
                    ObdServerBootstrap bootstrap = new ObdServerBootstrap(9999);
                } catch (InterruptedException e) {
                    log.error("异常:",e);
                }
            }}.start();

    }






    public synchronized boolean isLogined() {
        return isLogined;
    }

    public synchronized void setIsLogined(boolean isLogined) {
        this.isLogined = isLogined;
    }
}
