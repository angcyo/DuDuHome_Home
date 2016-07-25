package com.dudu.workflow.push.request;

import com.dudu.commonlib.CommonLib;
import com.dudu.rest.common.IpUtils;
import com.dudu.rest.common.RetrofitServiceFactory;
import com.dudu.rest.model.PushCallBackResponse;
import com.dudu.rest.model.common.RequestBody;
import com.dudu.rest.model.common.RequestResponse;
import com.dudu.rest.service.PushCallBackService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/5/23.
 */
public class PushCallBackRetrofitImpl implements PushCallBackRequest {

    private Logger logger = LoggerFactory.getLogger("PushCallBackRequest");

    @Override
    public Observable<RequestResponse> pushCallBack(String messageId, String method) {
        RequestBody requestBody = IpUtils.requestArgsToRequestBody(PushCallBackService.PUSH_CALLBACK, new PushCallBackResponse(method, messageId), CommonLib.getInstance().getObeId());
        logger.debug("推送回调requestBody:" + requestBody.params.toString());
        return RetrofitServiceFactory.getPushCallBackService().pushCallBack(CommonLib.getInstance().getObeId(), requestBody)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
