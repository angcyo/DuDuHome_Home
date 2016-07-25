package com.dudu.rest.service;

import com.dudu.rest.model.flow.VideoStreamBean;

import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

/**
 * Created by robi on 2016-06-22 18:14.
 */
public interface VideoStreamService {

    @GET("/done/obeStatus/mirrorRequest/{json}")
    Observable<VideoStreamBean> confirmMethod(@Path("json") String json);
}
