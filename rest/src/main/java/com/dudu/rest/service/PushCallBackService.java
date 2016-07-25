package com.dudu.rest.service;

import com.dudu.rest.model.common.RequestBody;
import com.dudu.rest.model.common.RequestResponse;

import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;

/**
 * Created by Administrator on 2016/5/21.
 */
public interface PushCallBackService {

    public static final String PUSH_CALLBACK = "/callBack/pushCallBack";

    //推送回调的接口
    @POST(PUSH_CALLBACK + "/{businessId}/mirror")
    public Observable<RequestResponse> pushCallBack(@Path("businessId") String businessId, @Body RequestBody requestBody);

}
