package com.dudu.workflow.vipNavi;

import com.dudu.rest.model.VipNavigationResponse;

import rx.Observable;

/**
 * Created by lxh on 2016-07-04 18:13.
 */
public interface VipNavigationRequest {

    public Observable<VipNavigationResponse> requestVipNavigation();

}
