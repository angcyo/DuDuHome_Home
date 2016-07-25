package com.dudu.voice.semantic.parser;

import com.aispeech.common.JSONResultParser;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.voice.semantic.bean.CmdBean;
import com.dudu.voice.semantic.bean.SemanticBean;
import com.dudu.voice.semantic.bean.VolumeBean;
import com.dudu.voice.semantic.bean.map.MapBean;
import com.dudu.voice.semantic.bean.map.MapNearbyBean;
import com.dudu.voice.semantic.constant.SemanticConstant;

/**
 * Created by 赵圣琪 on 2015/12/28.
 */
public class CloudJsonParser extends SemanticParser {

    private static boolean isSelfChecking(String text) {
        if ((text.contains("车辆字井")
                || text.contains("车辆自检")
                || text.contains("车辆自己"))
                || text.contains("车辆自件")) {
            return true;
        }

        return false;
    }

    private static CmdBean getSelfCheckingBean(String text) {
        String action = text.substring(0, 2);
        CmdBean bean = new CmdBean();
        bean.setHasResult(true);
        bean.setText(action + "车辆自检");
        bean.setService(SemanticConstant.SERVICE_CMD);
        bean.setAction(action);
        bean.setTarget(SemanticConstant.SELF_CHECKING_CN);
        return bean;
    }

    private static boolean guard(String text) {
        if ((text.length() <= 6) && (text.contains("防盗系统") || text.contains("打开防盗"))) {
            return true;
        }
        return false;
    }

    private static boolean robbery(String text) {
        if ((text.length() <= 6) && (text.contains("防劫系统") || text.contains("打开防劫"))) {
            return true;
        }
        return false;
    }

    @Override
    public SemanticBean getSemanticBean(String result) {

        mResultParser = new JSONResultParser(result);

        final String text = mResultParser.getInput();

        if (isSelfChecking(text)) {
            return getSelfCheckingBean(text);
        }

        if (robbery(text)) {
            return getRobberyBean(text);
        }

        if (guard(text)) {
            return getGuardBean(text);
        }

        if (isFlowPay(text)) {
            return getFlowPayBean(text);
        }

        if (isChangeWifiPwd(text)) {
            return getFlowPayBean(text);
        }

        if (isunlockGuard(text)) {
            return getGuardLockBean(text);
        }

        if (isOpenRobbery(text)) {
            return getopenRobberyBean(text);
        }

        if (isContacts(text)) {
            return getContacts(text);
        }

        if (isRear(text)) {
            return getRearCmdBean(text);
        }

        if (isVIPService(text)) {
            return getVIPCmdBean(text);
        }

        if (isTyre(text)) {
            return getTyreCmdBean(text);
        }

        if (isLookPicture(text)) {
            return getLookPictureCmdBean(text);
        }

        if (isChangeShot(text)) {
            return getChangeShotCmdBean(text);
        }

        if (isVolume_down(text)) {
            return getVolumeBeanBean(text, "-");
        }
        if (isVolume_up(text)) {
            return getVolumeBeanBean(text, "+");
        }
        if (isPlayVideo(text)) {
            return getPlayVideoBean(text);
        }
        if (isbank(text)) {
            return getBankBean(text);
        }

        mSemantics = mResultParser.getSemantics();

        if (mSemantics != null) {
            switch (getDomain()) {
                case SemanticConstant.DOMAIN_MAP:
                    return MapParser.parseMapBean(result);

                case SemanticConstant.DOMAIN_CAR_CONTROL:
                    return CarControlParser.parseCarControlBean(mSemantics, text);

                case SemanticConstant.DOMAIN_PHONE:
                    return PhoneParser.parsePhoneBean(mSemantics, text);

                case SemanticConstant.DOMAIN_WEATHER:
                    return WeatherParser.parseWeatherBean(mSemantics, text);
            }
        }
        return SemanticBean.getDefaultBean(text, getDomain());
    }


    private String getDomain() {
        try {
            if (mSemantics != null && !mSemantics.isNull("request")) {
                return mSemantics.getJSONObject("request").getString("domain");
            }
            return "";
        } catch (Exception e) {
            logger.error("解析json出错： " + e.getMessage());
        }
        return "";
    }

    private CmdBean getRobberyBean(String text) {
        CmdBean cmdBean = new CmdBean();
        cmdBean.setHasResult(true);
        cmdBean.setText(text);
        cmdBean.setService(SemanticConstant.SERVICE_CMD);
        cmdBean.setTarget(Constants.ROBBERY);
        return cmdBean;
    }

    private CmdBean getGuardBean(String text) {
        CmdBean cmdBean = new CmdBean();
        cmdBean.setHasResult(true);
        cmdBean.setText(text);
        cmdBean.setService(SemanticConstant.SERVICE_CMD);
        cmdBean.setTarget(Constants.GUARD);
        return cmdBean;
    }

    private boolean isFlowPay(String text) {

        if (text.length() >= 4 && (text.contains("充值") || text.contains("流量充值"))) {

            return true;
        }
        return false;
    }

    private CmdBean getFlowPayBean(String text) {
        CmdBean cmdBean = new CmdBean();
        cmdBean.setHasResult(true);
        cmdBean.setText(text);
        cmdBean.setService(SemanticConstant.SERVICE_CMD);
        cmdBean.setTarget(Constants.FLOWPAY);
        return cmdBean;
    }

    private CmdBean getGuardLockBean(String text) {
        CmdBean cmdBean = new CmdBean();
        cmdBean.setHasResult(true);
        cmdBean.setText(text);
        cmdBean.setService(SemanticConstant.SERVICE_CMD);
        cmdBean.setTarget(Constants.GUARDUNLOCK);
        return cmdBean;
    }

    private CmdBean getopenRobberyBean(String text) {
        CmdBean cmdBean = new CmdBean();
        cmdBean.setHasResult(true);
        cmdBean.setText(text);
        cmdBean.setService(SemanticConstant.SERVICE_CMD);
        cmdBean.setTarget(Constants.OPEN_ROBBERY);
        return cmdBean;
    }

    private boolean isChangeWifiPwd(String text) {

        if (text.contains("WIFI密码设置") || text.contains("修改WIFI密码") || text.contains("设置WIFI密码")) {
            return true;
        }
        return false;
    }

    private boolean isunlockGuard(String text) {
        if (text.contains("我要解锁") || text.contains("防盗解锁")) {
            return true;
        }
        return false;
    }

    private boolean isOpenRobbery(String text) {
        if (text.length() == 6 && text.contains("防劫模式")) {
            return true;
        }
        return false;
    }

    private boolean isContacts(String text) {
        if (text.equals("打开通讯录") || text.equals("打开电话本")) {
            return true;
        }
        return false;
    }

    private CmdBean getContacts(String text) {

        CmdBean cmdBean = new CmdBean();
        cmdBean.setHasResult(true);
        cmdBean.setText(text);
        cmdBean.setService(SemanticConstant.SERVICE_CMD);
        cmdBean.setTarget(Constants.CONTACT);
        return cmdBean;
    }

    private boolean isRear(String text) {

        if (text.contains("我要倒车") || text.contains("开始倒车") || text.contains("打开后录") || text.contains("后置预览") || text.contains("我要到车")) {
            return true;
        }
        return false;
    }

    private CmdBean getRearCmdBean(String text) {
        CmdBean cmdBean = new CmdBean();
        cmdBean.setHasResult(true);
        cmdBean.setText(text);
        cmdBean.setService(SemanticConstant.SERVICE_CMD);
        cmdBean.setTarget(Constants.REAR_RECOED);
        return cmdBean;
    }

    private boolean isVIPService(String text) {
        if (text.contains("拨打VIP") || text.contains("打开VIP") || text.contains("拨打贵宾热线") || text.contains("拨打热线")) {
            return true;
        }

        return false;
    }

    private CmdBean getVIPCmdBean(String text) {
        CmdBean cmdBean = new CmdBean();
        cmdBean.setHasResult(true);
        cmdBean.setText(text);
        cmdBean.setService(SemanticConstant.SERVICE_CMD);
        cmdBean.setTarget(Constants.VIPSERVICE);
        return cmdBean;
    }

    private boolean isTyre(String text) {
        if (text.contains("查看胎压信息") || text.contains("查看轮胎") || text.contains("查看轮胎") || text.contains("查看胎温")) {
            return true;
        }
        return false;
    }

    private CmdBean getTyreCmdBean(String text) {
        CmdBean cmdBean = new CmdBean();
        cmdBean.setHasResult(true);
        cmdBean.setText(text);
        cmdBean.setService(SemanticConstant.SERVICE_CMD);
        cmdBean.setTarget(Constants.TYRE);
        return cmdBean;
    }

    private boolean isLookPicture(String text) {
        if (text.contains("看照片") || text.contains("查看相册") || text.contains("打开相册")) {
            return true;
        }
        return false;
    }

    private CmdBean getLookPictureCmdBean(String text) {
        CmdBean cmdBean = new CmdBean();
        cmdBean.setHasResult(true);
        cmdBean.setText(text);
        cmdBean.setService(SemanticConstant.SERVICE_CMD);
        cmdBean.setTarget(Constants.LOOK_PICTURE);
        return cmdBean;
    }

    private boolean isChangeShot(String text) {
        if (text.contains("切换镜头") || text.contains("切换摄像头")) {
            return true;
        }
        return false;
    }

    private CmdBean getChangeShotCmdBean(String text) {
        CmdBean cmdBean = new CmdBean();
        cmdBean.setHasResult(true);
        cmdBean.setText(text);
        cmdBean.setAction(Constants.CHANGE_SHOT);
        cmdBean.setService(SemanticConstant.SERVICE_CMD);
        cmdBean.setTarget(SemanticConstant.RECORD_CN);
        return cmdBean;
    }

    private boolean isVolume_up(String text) {
        if (text.contains("声音太小了")) {
            return true;
        }
        return false;
    }

    private boolean isVolume_down(String text) {
        if (text.contains("声音太大了")) {
            return true;
        }
        return false;
    }

    private VolumeBean getVolumeBeanBean(String text, String operation) {
        VolumeBean bean = new VolumeBean();
        bean.setHasResult(true);
        bean.setText(text);
        bean.setOperation(operation);
        bean.setService(SemanticConstant.SERVICE_VOLUME);
        return bean;
    }

    private boolean isPlayVideo(String text) {
        if (text.contains("播放视频") || text.contains("播放录像")) {
            return true;
        }
        return false;
    }

    private CmdBean getPlayVideoBean(String text) {
        CmdBean cmdBean = new CmdBean();
        cmdBean.setHasResult(true);
        cmdBean.setText(text);
        cmdBean.setAction(Constants.PLAY);
        cmdBean.setService(SemanticConstant.SERVICE_CMD);
        cmdBean.setTarget(SemanticConstant.RECORD_CN);
        return cmdBean;
    }

    private boolean isbank(String text) {
        if (text.contains("我要取钱") || text.contains("我想取钱")) {
            return true;
        }
        return false;
    }

    private MapBean getBankBean(String text) {
        MapBean mapBean = new MapNearbyBean();
        mapBean.setPoiName("银行");
        mapBean.setHasResult(true);
        mapBean.setService(SemanticConstant.SERVICE_NEARBY);
        mapBean.setText(text);
        return mapBean;
    }

}
