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
 * Created by Administrator on 2016/5/21.
 */
public interface PushCallBackRequest {

    //推送回调的接口
    public Observable<RequestResponse> pushCallBack(String messageId, String method);

}
