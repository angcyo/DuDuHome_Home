package com.dudu.map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.dudu.aios.ui.map.AddressSearchActivity;
import com.dudu.aios.ui.map.GaodeMapActivity;
import com.dudu.aios.ui.map.MapDialog;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.ActivitiesManager;
import com.dudu.android.launcher.utils.CommonAddressUtil;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.android.launcher.utils.NetworkUtils;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.event.ChooseEvent;
import com.dudu.event.MapResultShow;
import com.dudu.monitor.repo.location.LocationManage;
import com.dudu.navi.NavigationManager;
import com.dudu.navi.Util.NaviUtils;
import com.dudu.navi.entity.Navigation;
import com.dudu.navi.entity.PoiResultInfo;
import com.dudu.navi.entity.Point;
import com.dudu.navi.event.NaviEvent;
import com.dudu.navi.vauleObject.NaviDriveMode;
import com.dudu.navi.vauleObject.NavigationType;
import com.dudu.navi.vauleObject.SearchType;
import com.dudu.rest.model.VipNavigationResponse;
import com.dudu.voice.FloatWindowUtils;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.SceneType;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.voice.semantic.engine.SemanticEngine;
import com.dudu.workflow.common.RequestFactory;
import com.umeng.analytics.MobclickAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by lxh on 2015/11/26.
 */
public class NavigationProxy {

    public static final int OPEN_MANUAL = 1;
    public static final int OPEN_VOICE = 2;
    public static final int OPEN_MAP = 3;

    public static final String ONLY_SHOWSTRATEGY = "ONLY_SHOWSTRATEGY";
    private static final int REMOVE_WINDOW_TIME = 6 * 1000;
    private static NavigationProxy mInstance;
    public Point endPoint = null;
    public Subscription naviSubscription = null;
    private Context mContext;
    private NavigationManager navigationManager;
    private VoiceManagerProxy voiceManager;
    private int chooseStep;
    private boolean isManual = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private MapDialog waitingDialog = null;
    private String msg;
    private long mLastClickTime = 0;
    private Runnable removeWindowRunnable = () -> FloatWindowUtils.getInstance().removeFloatWindow();
    private boolean needNotify = true;
    private boolean isShowList = false;
    private boolean isStartNewNavi = false;
    private int requestCount = 0;
    private Subscription requestPositionSub = null;
    private Subscription requestSub = null;

    public void setNeedNotify(boolean needNotify) {
        this.needNotify = needNotify;
    }

    public boolean isNeedRefresh() {
        return needRefresh;
    }

    public void setNeedRefresh(boolean needRefresh) {

        this.needRefresh = needRefresh;
    }

    private boolean needRefresh = true;

    private Logger logger = LoggerFactory.getLogger("naviInfo");

    public boolean isStartNewNavi() {
        return isStartNewNavi;
    }

    public void setStartNewNavi(boolean startNewNavi) {
        isStartNewNavi = startNewNavi;
    }

    private View.OnClickListener cancel = v -> {

        disMissProgressDialog();
        needNotify = false;
        voiceManager.stopSpeaking();
        voiceManager.stopUnderstanding();
        MobclickAgent.onEvent(mContext, ClickEvent.click40.getEventId());
    };

    public NavigationProxy() {
        mContext = LauncherApplication.getContext();

        navigationManager = NavigationManager.getInstance(mContext);

        EventBus.getDefault().register(this);

        voiceManager = VoiceManagerProxy.getInstance();

    }

    public static NavigationProxy getInstance() {
        if (mInstance == null) {
            mInstance = new NavigationProxy();
        }
        return mInstance;
    }

    public boolean isShowList() {

        return isShowList;
    }

    public void setShowList(boolean showList) {
        isShowList = showList;
    }

    public boolean isManual() {
        return isManual;
    }

    public int getChooseStep() {
        return chooseStep;
    }

    public void setChooseStep(int chooseStep) {
        this.chooseStep = chooseStep;
    }

    public void setIsManual(boolean isManual) {
        this.isManual = isManual;
    }

    public boolean openNavi(int openType) {
        if (checkFastClick()) {
            return false;
        }
        switch (NaviUtils.getOpenMode(mContext)) {
            case INSIDE:
                return (openType == OPEN_MAP) ? openMapActivity() : openActivity(openType);
            case OUTSIDE:
                openGaode();
                break;
        }
        return true;
    }

    private boolean checkFastClick() {
        long now = System.currentTimeMillis();
        if (now - mLastClickTime < 3000) {
            return true;
        }
        mLastClickTime = now;
        return false;
    }

    public void closeMap() {
        ActivitiesManager.getInstance().closeTargetActivity(
                GaodeMapActivity.class);
    }

    private boolean openMapActivity() {
        intentActivity(GaodeMapActivity.class);
        return true;
    }

    private boolean openActivity(int openType) {

        if (navigationManager.isNavigatining() || !NetworkUtils.isNetworkConnected(LauncherApplication.getContext())) {
            FloatWindowUtils.getInstance().removeFloatWindow();
            openGaode();
        } else {
            if (!isMapActivity() && !isShowList) {
                intentActivity(GaodeMapActivity.class);
                if (openType == OPEN_VOICE) {
                    MobclickAgent.onEvent(mContext, ClickEvent.voice12.getEventId());
                    handler.postDelayed(() -> {
                        voiceManager.startSpeaking(mContext.getString(R.string.openNavi_notice),
                                TTSType.TTS_START_UNDERSTANDING, true);
                        SemanticEngine.getProcessor().switchSemanticType(SceneType.NAVIGATION);
                    }, 1000);
                }
            } else {
                return false;
            }
        }

        return true;
    }

    private Activity getTopActivity() {
        return ActivitiesManager.getInstance().getTopActivity();
    }

    private boolean isMapActivity() {
        return (getTopActivity() != null && getTopActivity() instanceof GaodeMapActivity);
    }

    public void existNavi() {
        if (navigationManager.isNavigatining()) {
            logger.debug("naviproxy 退出导航");
            GaodeMapAppUtil.exitGapdeApp();
            navigationManager.existNavigation();
        }
    }

    public void searchControl(String keyword, SearchType type) {
        if (navigationManager.getSearchType() == SearchType.SEARCH_COMMONADDRESS)
            type = SearchType.SEARCH_COMMONPLACE;
        navigationManager.setSearchType(type);
        navigationManager.setKeyword(keyword);
        if (type == SearchType.SEARCH_COMMONADDRESS) {
            return;
        }
        doSearch();
    }

    public void doSearch() {
        FloatWindowUtils.getInstance().removeFloatWindow();
        naviSubscription = null;
        if (!NetworkUtils.isNetworkConnected(LauncherApplication.getContext())) {
            openGaode();
            return;
        }
        if (!isShowList && !isMapActivity()) {
            intentActivity(GaodeMapActivity.class);
        }
        switch (navigationManager.getSearchType()) {
            case SEARCH_DEFAULT:
                return;
            case SEARCH_NEARBY:
            case SEARCH_NEAREST:
            case SEARCH_PLACE:
            case SEARCH_COMMONPLACE:
                handler.postDelayed(() -> searchHint(), 500);
                break;
        }

    }

    private void searchHint() {
        msg = String.format(mContext.getString(R.string.navi_searching),navigationManager.getKeyword());
        boolean isShow = false;
        if (TextUtils.isEmpty(navigationManager.getKeyword())) {
            voiceManager.stopUnderstanding();
            voiceManager.startSpeaking(mContext.getString(R.string.navi_search_keyword),
                    TTSType.TTS_START_UNDERSTANDING, true);
            return;
        }

        if (LocationManage.getInstance().getCurrentLocation() == null) {
            navigationManager.setSearchType(SearchType.SEARCH_DEFAULT);
            voiceManager.startSpeaking(mContext.getString(R.string.notice_location), TTSType.TTS_DO_NOTHING, true);
            removeWindow();
            return;
        }
        needNotify = true;
        if (Constants.CURRENT_POI.equals(navigationManager.getKeyword())) {
            navigationManager.setSearchType(SearchType.SEARCH_CUR_LOCATION);
            msg = mContext.getString(R.string.notice_search_location);
            isShow = true;
        }
        if (!isManual) {
            voiceManager.startSpeaking(msg, TTSType.TTS_DO_NOTHING, isShow);
        }
        handler.postDelayed(() -> {
            needRefresh = true;
            showProgressDialog(mContext.getString(R.string.searching));
            navigationManager.search();
        }, 1500);
    }

    public void onEventMainThread(NaviEvent.NaviVoiceBroadcast event) {
        if (isManual)
            return;
        navigationManager.getLog().debug("NaviVoiceBroadcast stopUnderstanding");
        voiceManager.clearMisUnderstandCount();
        voiceManager.stopUnderstanding();
        removeCallback();
        voiceManager.startSpeaking(event.getNaviVoice(), TTSType.TTS_START_UNDERSTANDING, true);
    }

    public void onEventMainThread(NaviEvent.SearchResult event) {
        removeCallback();

        switch (event.getType()) {
            case SUCCESS:
                handlerPoiResultSuccess();
                break;
            case FAIL:
                handleResultFail(event.getInfo());
                break;
        }

        disMissProgressDialog();
    }


    private void handleResultFail(String text) {
        navigationManager.setSearchType(SearchType.SEARCH_DEFAULT);
        if (needNotify) {
            voiceManager.clearMisUnderstandCount();
            voiceManager.stopUnderstanding();
            voiceManager.startSpeaking(text, TTSType.TTS_START_UNDERSTANDING, true);
        }
        if (!isShowList) {
            SemanticEngine.getProcessor().switchSemanticType(SceneType.HOME);
        }
    }

    public void onEventMainThread(NavigationType event) {

        try {
            disMissProgressDialog();
        } catch (Exception e) {

        } finally {

            removeCallback();
            naviSubscription = null;
            switch (event) {
                case CALCULATEERROR:
                    if (!needNotify)
                        return;
                    navigationManager.setSearchType(SearchType.SEARCH_DEFAULT);
                    voiceManager.startSpeaking("路径规划出错，请稍后再试", TTSType.TTS_DO_NOTHING, true);
                    removeWindow();
                    return;
                case NAVIGATION_END:
                    navigationManager.setNavigationType(NavigationType.DEFAULT);
                    intentActivity(GaodeMapActivity.class);
                    break;
            }
            navigationManager.setSearchType(SearchType.SEARCH_DEFAULT);

        }

    }

    public void handlerPoiResultSuccess() {

        if (!needNotify) {
            return;
        }
        switch (navigationManager.getSearchType()) {
            case SEARCH_CUR_LOCATION:
                handler.postDelayed(() -> voiceManager.startSpeaking(navigationManager.getCurlocationDesc(),
                        TTSType.TTS_START_UNDERSTANDING, true), 200);
                navigationManager.setSearchType(SearchType.SEARCH_DEFAULT);
                MobclickAgent.onEvent(mContext, ClickEvent.voice14.getEventId());
                break;

            case SEARCH_NEAREST:
                endPoint = null;
                SemanticEngine.getProcessor().switchSemanticType(
                        SceneType.MAP_CHOISE);
                this.endPoint = new Point(navigationManager.getPoiResultList().get(0).getLatitude(),
                        navigationManager.getPoiResultList().get(0).getLongitude());
                EventBus.getDefault().post(MapResultShow.STRATEGY);
                navigationManager.setSearchType(SearchType.SEARCH_DEFAULT);

                return;
            default:
                endPoint = null;
                SemanticEngine.getProcessor().switchSemanticType(
                        SceneType.MAP_CHOISE);
                EventBus.getDefault().post(MapResultShow.ADDRESS);
                break;

        }

    }

    private void initWaitingDialog(String message) {
        if (waitingDialog != null && waitingDialog.isShowing()) {
            waitingDialog.dismiss();
            waitingDialog = null;

        }
        waitingDialog = new MapDialog(ActivitiesManager.getInstance().getTopActivity(), message, cancel);
        Window dialogWindow = waitingDialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.x = 10; // 新位置X坐标
        lp.y = 0; // 新位置Y坐标
        lp.width = 306;
        lp.height = 218;
        dialogWindow.setAttributes(lp);
    }

    /**
     * 显示进度框
     */
    public void showProgressDialog(String message) {
        try {
            initWaitingDialog(message);
            waitingDialog.show();
            switch (navigationManager.getSearchType()) {
                case SEARCH_CUR_LOCATION:
                    return;
            }
        } catch (Exception e) {

        }

        FloatWindowUtils.getInstance().removeFloatWindow();
    }

    public void disMissProgressDialog() {
        if (waitingDialog != null && waitingDialog.isShowing()) {
            waitingDialog.dismiss();
            waitingDialog = null;

        }
    }

    /**
     * 添加常用地
     *
     * @param choosePoint
     */
    public void addCommonAddress(final PoiResultInfo choosePoint) {
        final String addType = navigationManager.getCommonAddressType().getName();
        CommonAddressUtil.setCommonAddress(addType, mContext, choosePoint.getAddressTitle());
        CommonAddressUtil.setCommonLocation(addType,
                mContext, choosePoint.getLatitude(), choosePoint.getLongitude());

        voiceManager.stopUnderstanding();

        handler.postDelayed(() -> voiceManager.startSpeaking("添加" + choosePoint.getAddressTitle() + "为 " + addType + " 地址成功,是否要开始导航",
                TTSType.TTS_START_UNDERSTANDING, true), 200);
        navigationManager.setSearchType(SearchType.SEARCH_DEFAULT);
        SemanticEngine.getProcessor().switchSemanticType(SceneType.COMMON_NAVI);
        MobclickAgent.onEvent(mContext, ClickEvent.voice32.getEventId());

    }

    public void onCommonAdrNavi() {
        Navigation navigation = new Navigation(endPoint, NaviDriveMode.SPEEDFIRST, NavigationType.NAVIGATION);
        startNavigation(navigation);
    }

    public void onNextPage() {
        MobclickAgent.onEvent(mContext, ClickEvent.voice30.getEventId());
        EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.NEXT_PAGE, 0));
    }

    public void onPreviousPage() {
        EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.PREVIOUS_PAGE, 0));
    }

    public void onChoosePage(int page) {
        MobclickAgent.onEvent(mContext, ClickEvent.voice29.getEventId());
        EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.CHOOSE_PAGE, page));
    }

    public void onChooseNumber(int position) {

        if (chooseStep == 1) {
            MobclickAgent.onEvent(mContext, ClickEvent.voice28.getEventId());
            EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.CHOOSE_NUMBER, position));
        } else if (chooseStep == 2) {
            MobclickAgent.onEvent(mContext, ClickEvent.voice31.getEventId());
            EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.STRATEGY_NUMBER, position));
        }
    }

    public void onLastPage() {
        EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.LAST_PAGE, 0));

    }

    public void onLastOne() {
        if (chooseStep == 1) {
            EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.CHOOSE_NUMBER, navigationManager.getPoiResultList().size()));
        } else if (chooseStep == 2) {
            EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.STRATEGY_NUMBER, 6));
        }
    }

    public void startNavigation(Navigation navigation) {
        if (naviSubscription != null || navigation == null)
            return;
        naviSubscription = Observable.just(navigation)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(navigation1 -> {
                    MobclickAgent.onEvent(mContext, ClickEvent.voice15.getEventId());
                    VoiceManagerProxy.getInstance().stopSpeaking();
//                    FloatWindowUtils.getInstance().removeFloatWindow();
                    SemanticEngine.getProcessor().switchSemanticType(SceneType.HOME);
                    isManual = false;
                    if (navigationManager.isNavigatining()) {
                        logger.debug("导航中，开始新导航");
                        isStartNewNavi = true;
                    }
                    GaodeMapAppUtil.startNavi(navigation1);
                    ActivitiesManager.getInstance().closeTargetActivity(GaodeMapActivity.class);
                    ActivitiesManager.getInstance().closeTargetActivity(AddressSearchActivity.class);

                }, throwable -> logger.error("startNavigation", throwable));
        Observable.timer(1, TimeUnit.SECONDS)
                .subscribe(aLong -> {
                    naviSubscription = null;
                }, throwable -> logger.error("startNavigation", throwable));
    }


    public void removeCallback() {
        if (handler != null && removeWindowRunnable != null) {
            handler.removeCallbacks(removeWindowRunnable);
        }
    }

    public void removeWindow() {
        disMissProgressDialog();
        if (navigationManager.getSearchType() == SearchType.SEARCH_DEFAULT) {
            handler.postDelayed(removeWindowRunnable, REMOVE_WINDOW_TIME);
        }
    }

    private void intentActivity(Class intentClass) {
        Intent standIntent = new Intent(mContext, intentClass);
        standIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        mContext.startActivity(standIntent);
        mLastClickTime = 0;
    }

    public void openGaode() {
        if (!NetworkUtils.isNetworkConnected(LauncherApplication.getContext()) && !navigationManager.isNavigatining()) {
            VoiceManagerProxy.getInstance().startSpeaking(mContext.getString(R.string.notice_off_line_navi), TTSType.TTS_DO_NOTHING, false);
            EventBus.getDefault().post(NaviEvent.FloatButtonEvent.SHOW);
        }
        GaodeMapAppUtil.openGaode();
        SemanticEngine.getProcessor().switchSemanticType(SceneType.HOME);
    }


    public void getVipPosition() {

        if (requestPositionSub != null) {
            requestPositionSub.unsubscribe();
            requestPositionSub = null;
        }
        requestPositionSub = RequestFactory.getVipNavigationRequest().requestVipNavigation()
                .subscribeOn(Schedulers.newThread())
                .subscribe(vipNavigationResponse -> {
                    if (vipNavigationResponse != null) {
                        logger.debug("resultCode:{},{}", vipNavigationResponse.resultCode, vipNavigationResponse.resultMsg);
                        if (vipNavigationResponse.resultCode != 0 && requestCount < 3) {
                            reQuery();
                        } else {
                            vipNavi(vipNavigationResponse);
                        }
                    }
                }, throwable -> {
                    logger.debug("request error ", throwable);
                });

    }

    private void reQuery() {
        if (requestSub != null) {
            requestSub.unsubscribe();
            requestSub = null;
        }
        requestSub = Observable.timer(3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe(aLong -> {
                    getVipPosition();
                    requestCount++;
                }, throwable -> logger.error("requestSub", throwable));
    }

    private void vipNavi(VipNavigationResponse vip) {
        if (vip != null && vip.result != null) {
            logger.debug("vipNavi lat[{}],lon[{}],position:{}", vip.result.getLat(), vip.result.getLon(), vip.result.getPosition());
            double lat = Double.parseDouble(vip.result.getLat());
            double lon = Double.parseDouble(vip.result.getLon());
            endPoint = new Point(lat, lon);
            needRefresh = true;
            handler.postDelayed(() -> {
                String playText = String.format(mContext.getString(R.string.vipNavi), vip.result.getPosition());
                VoiceManagerProxy.getInstance().startSpeaking(playText, TTSType.TTS_START_UNDERSTANDING, false);
                Intent intent = new Intent(mContext, AddressSearchActivity.class);
                intent.putExtra(ONLY_SHOWSTRATEGY, true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                requestCount = 0;
            }, 1000);

        }

    }

}
