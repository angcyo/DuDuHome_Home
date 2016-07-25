package com.dudu.network.message;

import com.dudu.commonlib.CommonLib;
import com.dudu.network.message.id.Bicker;
import com.dudu.network.message.id.BusinessMessageEnum;
import com.dudu.obd.common.AskMsg;
import com.dudu.obd.common.AskParams;
import com.dudu.obd.common.BusinessCode;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *  OBD adc电压上传消息类
 *  (ps: 后面这种单个字符串数据的可以做成一个通用类)
 * Created by Robert on 2016/6/22.
 */
public class VoltageDataUpload extends MessagePackage<AskMsg>{
    private AskMsg askMsg;
    private String obeId;//obeid
    private String extData; //voltage

    public VoltageDataUpload() {
        askMsg = new AskMsg();
        askMsg.setMessageId(Bicker.getBusinessCode(BusinessMessageEnum.VOLTAGE_DATA.getCode()));
        askMsg.setCode(BusinessCode.VOLTAGE_UPLOAD);


    }

    public VoltageDataUpload(String data) {
        askMsg = new AskMsg();
        askMsg.setMessageId(Bicker.getBusinessCode(BusinessMessageEnum.VOLTAGE_DATA.getCode()));
        askMsg.setCode(BusinessCode.VOLTAGE_UPLOAD);

        obeId = CommonLib.getInstance().getObeId();
        this.extData = data;

        AskParams askParams = new AskParams();
        askParams.setContext(toJsonString());

        askMsg.setParams(askParams);
    }

    @Override
    public void setMessageId(String messageId) {
        askMsg.setMessageId(messageId);
    }

    @Override
    public String getMessageId() {
        return askMsg.getMessageId();
    }

    @Override
    public boolean isNeedWaitResponse() {
        return true;
    }

    @Override
    public boolean isNeedEncrypt() {
        return false;
    }

    @Override
    public void createFromJsonString(String messageJsonString) {
        try {
            JSONObject jsonObject = new JSONObject(messageJsonString);
            askMsg.setMessageId(jsonObject.getString("messageId"));
            askMsg.setCode(BusinessCode.VOLTAGE_UPLOAD);
            obeId =  jsonObject.getString("obeId");

            JSONObject jObj= new JSONObject();
            jObj.put("a", jsonObject.get("a"));
            jObj.put("b", jsonObject.get("b"));
            extData = jObj.toString();

            AskParams askParams = new AskParams();
            askParams.setContext(toJsonString());
            askMsg.setParams(askParams);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String toJsonString() {
        JSONObject sendJsonObject =  new JSONObject();
        try {
            sendJsonObject.put("messageId", askMsg.getMessageId());
            sendJsonObject.put("obeId", obeId);

            JSONObject data = new JSONObject(extData);
            sendJsonObject.put("a",data.get("a"));
            sendJsonObject.put("b",data.get("b"));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return sendJsonObject.toString();
    }

    @Override
    public boolean isNeedCache() {
        return true;
    }

    @Override
    public AskMsg getMessageEntity() {
        return askMsg;
    }

    @Override
    public void setMessageEntity(AskMsg messageEntity) {
        askMsg = messageEntity;
    }

    @Override
    public String getBusinessCode() {
        return askMsg.getCode().getCode();
    }

    @Override
    public void setBusinessCode(String businessCode) {

    }
}
