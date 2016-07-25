package com.dudu.rest.service;

import com.dudu.rest.model.common.RequestBody;
import com.dudu.rest.model.common.RequestResponse;

import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;

/**
 *  水温告警上传服务
 * Created by Robert on 2016/7/6.
 */
public interface WaterWarningService {

    public static final String WATER_WARNING = "warning/waterTemperature";

    @POST(WATER_WARNING + "/{businessId}/{platform}")
    public Observable<RequestResponse> uploadWaterWarning(@Path("businessId") String businessId, @Path("platform") String platform, @Body RequestBody requestBody);
}
