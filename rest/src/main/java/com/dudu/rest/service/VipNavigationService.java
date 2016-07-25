package com.dudu.rest.service;

import com.dudu.rest.model.VipNavigationResponse;
import com.dudu.rest.model.common.RequestBody;

import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;

/**
 * Created by lxh on 2016-07-04 18:21.
 */
public interface VipNavigationService {

    public static final String REQUEST_POSITION = "/voiceassistant/gainNavigationPosition";

    @POST("/voiceassistant/gainNavigationPosition/{businessId}/mirror")
    Observable<VipNavigationResponse> requestPosition(@Path("businessId") String businessId, @Body RequestBody body);
}
