package com.dudu.workflow.tpms;

import com.dudu.persistence.factory.RealmCallFactory;
import com.dudu.persistence.realm.RealmCallBack;
import com.dudu.persistence.realmmodel.tirepressure.TireInfoSetDataRealm;

/**
 * 胎压信息设置管理类
 * (主要用于设置或者回调胎压上下限值之用)
 * <p>
 * Created by Robert on 2016/6/30.
 */
public class TireInfoSetManager {

    private static TireInfoSetManager mTireInfoSetManager = null;
    private OnTireInfoUpdateListener mOnTireInfoUpdateListener = null;

    private String method = "";
    private String messageId = "";

    /**
     * 胎压信息设置管理单例模式
     *
     * @return
     */
    public static TireInfoSetManager getInstance() {
        if (mTireInfoSetManager == null) {
            mTireInfoSetManager = new TireInfoSetManager();
        }
        return mTireInfoSetManager;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * 设置胎压信息设置变动监听器
     *
     * @param listener
     */
    public void setOnTireInfoUpdateListener(OnTireInfoUpdateListener listener) {
        if (listener != null) {
            mOnTireInfoUpdateListener = listener;
        }
    }

    /**
     * 通知胎压信息设置参数更新事件
     *
     * @param tireInfoSet
     */
    public void notifyTireInfoSetUpdate(TireInfoSetDataRealm tireInfoSet) {
        if (mOnTireInfoUpdateListener != null) {
            mOnTireInfoUpdateListener.onUpdate(tireInfoSet);
        }
    }

    /**
     * 同步查找胎压信息设置参数
     *
     * @param realmCallBack
     */
    public void getTireInfoSetInfo(RealmCallBack<TireInfoSetDataRealm, Exception> realmCallBack) {
        RealmCallFactory.findTireInfoSetDatasSync(realmCallBack);
    }

    /**
     * 胎压信息设置监听器接口
     */
    public interface OnTireInfoUpdateListener {
        void onUpdate(TireInfoSetDataRealm tireInfoSet);
    }
}
