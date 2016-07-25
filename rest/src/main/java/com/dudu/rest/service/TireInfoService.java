package com.dudu.rest.service;

import com.dudu.rest.model.common.RequestBody;
import com.dudu.rest.model.common.RequestResponse;

import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;

/**
 * 实时胎压信息上传服务
 * Created by Robert on 2016/6/28.
 */
public interface TireInfoService {

    public static final String TIRE_PRESSURE = "tirePressure/uploadWarningValue";

    @POST(TIRE_PRESSURE + "/{businessId}/{platform}")
    public Observable<RequestResponse> uploadTirePressure(@Path("businessId") String businessId,@Path("platform") String platform, @Body RequestBody requestBody);
}
