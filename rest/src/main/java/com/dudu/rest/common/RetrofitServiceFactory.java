package com.dudu.rest.common;

import com.dudu.commonlib.utils.IPConfig;
import com.dudu.rest.service.ActiveService;
import com.dudu.rest.service.AppService;
import com.dudu.rest.service.BaiduWeatherService;
import com.dudu.rest.service.DrivingService;
import com.dudu.rest.service.FlowService;
import com.dudu.rest.service.GuardService;
import com.dudu.rest.service.PortalService;
import com.dudu.rest.service.PushCallBackService;
import com.dudu.rest.service.RobberyService;
import com.dudu.rest.service.TireInfoService;
import com.dudu.rest.service.VideoStreamService;
import com.dudu.rest.service.VipNavigationService;
import com.dudu.rest.service.WaterWarningService;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Administrator on 2016/2/15.
 */
public class RetrofitServiceFactory {

    private static Retrofit getRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(IPConfig.getInstance().getSERVER_ADDRESS())//设置服务端地址
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())//设置支持RX数据响应
                .addConverterFactory(GsonConverterFactory.create())//使用Gson封装、解析数据
                .build();
    }

    public static Retrofit getBaiduApiRetrofit() {
        return new Retrofit.Builder()
                .baseUrl("http://apis.baidu.com")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())//设置支持RX数据响应
                .addConverterFactory(GsonConverterFactory.create())//使用Gson封装、解析数据
                .build();
    }

    public static RobberyService getRobberyService() {
        return getRetrofit().create(RobberyService.class);
    }

    public static GuardService getGuardService() {
        return getRetrofit().create(GuardService.class);
    }

    public static PushCallBackService getPushCallBackService() {
        return getRetrofit().create(PushCallBackService.class);
    }

    public static DrivingService getDrivingService() {
        return getRetrofit().create(DrivingService.class);
    }

    public static FlowService getFlowService() {
        return getRetrofit().create(FlowService.class);
    }

    public static ActiveService getActiveService() {
        return getRetrofit().create(ActiveService.class);
    }


    public static PortalService getPortalService() {
        return getRetrofit().create(PortalService.class);
    }

    public static AppService getAppService() {
        return getRetrofit().create(AppService.class);
    }

    public static BaiduWeatherService getBaiduWeatherService() {
        return getBaiduApiRetrofit().create(BaiduWeatherService.class);
    }


    //视频推流接口
    public static VideoStreamService getVideoStreamService() {
        return getRetrofit().create(VideoStreamService.class);
    }

    public static VipNavigationService getVipNavigationService() {
        return getRetrofit().create(VipNavigationService.class);
    }

    /**
     * 获取胎压信息网络访问服务
     *
     * @return
     */
    public static TireInfoService getTireInfoService() {
        return getRetrofit().create(TireInfoService.class);
    }

    /**
     *  获取水温告警信息网络访问服务
     * @return
     */
    public static WaterWarningService getWaterWarningService() {
        return getRetrofit().create(WaterWarningService.class);
    }

}

