package com.dudu.workflow.tpms;

import com.dudu.commonlib.CommonLib;
import com.dudu.rest.common.IpUtils;
import com.dudu.rest.common.RetrofitServiceFactory;
import com.dudu.rest.model.common.RequestBody;
import com.dudu.rest.model.common.RequestResponse;
import com.dudu.rest.service.TireInfoService;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * 胎压信息上传实现类
 * Created by Robert on 2016/6/28.
 */
public class TireInfoRequestRetrofitImpl implements TireInfoRequest {
    @Override
    public Observable<RequestResponse> notifyTireInfo(TirePressureData tirePressureData) {

        TireInfoData tireInfoData = new TireInfoData(tirePressureData);
        RequestBody requestBody = IpUtils.requestArgsToRequestBody(TireInfoService.TIRE_PRESSURE, tireInfoData, CommonLib.getInstance().getObeId());

        return RetrofitServiceFactory.getTireInfoService()
                .uploadTirePressure(CommonLib.getInstance().getObeId(),"mirror",requestBody)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());

    }
}
