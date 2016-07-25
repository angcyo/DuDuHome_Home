package com.dudu.workflow.obd;

import com.dudu.rest.model.common.RequestResponse;

import rx.Observable;

/**
 * Created by Robert on 2016/7/6.
 */
public interface WaterWarningRequest {

    Observable<RequestResponse> uploadWaterWarning(WaterWarningData coolantTemperatureData);
}
