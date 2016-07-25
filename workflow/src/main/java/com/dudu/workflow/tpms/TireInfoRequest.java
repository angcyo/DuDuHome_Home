package com.dudu.workflow.tpms;

import com.dudu.rest.model.common.RequestResponse;

import rx.Observable;

/**
 * 胎压信息上传接口
 * Created by Robert on 2016/6/28.
 */
public interface TireInfoRequest {

    Observable<RequestResponse> notifyTireInfo(TirePressureData tireInfo);
}
