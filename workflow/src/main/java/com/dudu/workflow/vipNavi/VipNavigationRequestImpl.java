package com.dudu.workflow.vipNavi;

import com.dudu.commonlib.CommonLib;
import com.dudu.rest.common.IpUtils;
import com.dudu.rest.common.RetrofitServiceFactory;
import com.dudu.rest.model.VipNavigationResponse;
import com.dudu.rest.model.common.RequestBody;
import com.dudu.rest.service.VipNavigationService;

import rx.Observable;

/**
 * Created by lxh on 2016-07-04 18:13.
 */
public class VipNavigationRequestImpl implements VipNavigationRequest{
    @Override
    public Observable<VipNavigationResponse> requestVipNavigation() {
        RequestBody requestBody = IpUtils.requestArgsToRequestBody(VipNavigationService.REQUEST_POSITION, null, CommonLib.getInstance().getObeId());
        return RetrofitServiceFactory
                .getVipNavigationService()
                .requestPosition(CommonLib.getInstance().getObeId(),requestBody);
    }
}
