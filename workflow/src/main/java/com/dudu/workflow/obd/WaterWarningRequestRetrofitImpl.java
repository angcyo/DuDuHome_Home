package com.dudu.workflow.obd;

import com.dudu.commonlib.CommonLib;
import com.dudu.rest.common.IpUtils;
import com.dudu.rest.common.RetrofitServiceFactory;
import com.dudu.rest.model.common.RequestBody;
import com.dudu.rest.model.common.RequestResponse;
import com.dudu.rest.service.WaterWarningService;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Robert on 2016/7/6.
 */
public class WaterWarningRequestRetrofitImpl implements  WaterWarningRequest {
    @Override
    public Observable<RequestResponse> uploadWaterWarning(WaterWarningData waterWarningData) {

        RequestBody requestBody = IpUtils.requestArgsToRequestBody(WaterWarningService.WATER_WARNING, waterWarningData, CommonLib.getInstance().getObeId());

        return RetrofitServiceFactory.getWaterWarningService()
                .uploadWaterWarning(CommonLib.getInstance().getObeId(),"mirror",requestBody)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
