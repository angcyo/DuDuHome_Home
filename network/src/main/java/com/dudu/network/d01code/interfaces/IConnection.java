package com.dudu.network.d01code.interfaces;

import com.dudu.network.valueobject.ConnectionParam;

/**
 * Created by dengjun on 2015/11/29.
 * Description :
 */
public interface IConnection {
    public boolean isConnected();

    public void connect(ConnectionParam connectionParam);

    public void disConnect();

    public void setConnectCallBack(IConnectCallBack iConnectCallBack);

    public void sendMessage(String sendMessage);
}
