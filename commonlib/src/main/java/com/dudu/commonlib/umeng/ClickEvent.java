package com.dudu.commonlib.umeng;

/**
 * Created by dengjun on 2016/6/29.
 * Description :
 */
public enum ClickEvent {
    CLICK_CARCHECKING("click1", "行车自检", "0"),
    CLICK_DRIVING_RECORD("click2", "行车记录", "0"),
    CLICK_MAP("click3", "导航定位", "0"),
    CLICK_VOICE("click4", "语音", "0"),
    CLICK_BLUETOOTH_CALL("click5", "蓝牙电话", "0"),
    CLICK_FLOW("click6", "移动热点", "0"),
    CLICK_ROBBERY_GUARD("click7", "防盗防劫", "0"),
    CLICK_VIP("click8", "VIP服务", "0"),
    CLICK_APP_DOWNLOAD("click9", "app下载", "0"),//未实现
    CLICK_GOTO4S("click10", "行车自检-前往检修", "0"),
    CLICK_CLEARFAULTS("click11", "行车自检-清除故障码", "0"),
    CLICK_SHOWFAULTS("click12", "行车自检-查看故障码", "0"),
    CLICK_PAIRSTART("click13", "行车自检-轮胎对码", "0"),
    CLICK_SHOPITEM("click14", "行车自检-汽修店列表项", "0"),

    DRIVE_CHANGE_SHOT("click15", "行车-切换镜头", "0"),
    DRIVE_TAKE_PICTURE("click16", "行车-拍照", "0"),
    DRIVE_VIDEO_LIST("click17", "行车-视频列表", "0"),
    DRIVE_PICTURE_SCAN("click18", "行车-图片预览", "0"),
    DRIVE_VIDEO_FRONT("click19", "行车-视频-前置录像", "0"),
    DRIVE_VIDEO_REAR("click20", "行车-视频-后置录像", "0"),
    DRIVE_VIDEO_ITEM("click21", "行车-视频列表项", "0"),
    DRIVE_VIDEO_DELETE("click22", "行车-视频删除", "0"),
    DRIVE_VIDEO_LEFT_SLILDE("click23", "行车-视频左滑", "0"),
    DRIVE_VIDEO_RIGHT_SLILDE("click24", "行车-视频右滑", "0"),
    DRIVE_VIDEO_PLAY_LAST_PAGE("click25", "行车-视频播放上一页", "0"),
    DRIVE_VIDEO_PLAY_NEXT_PAGE("click26", "行车-视频播放下一页", "0"),
    DRIVE_VIDEO_START_PALY("click27", "行车-视频开始播放", "0"),
    DRIVE_VIDEO_START_PAUSE("click28", "行车-视频暂停播放", "0"),
    DRIVE_VIDEO_DRAG_PROGRESS("click29", "行车-视频-拖拽进度", "0"),
    DRIVI_VIDEO_CLICK_PROGRESS("click30", "行车-视频-点击进度", "0"),
    DRIVE_PICTURE_ITEM("click31", "行车-图片列表项", "0"),
    DRIVE_PICTURE_EDIT("click32", "行车-图片-编辑", "0"),
    DRIVE_PICTURE_DELETE("click33", "行车-图片-删除", "0"),
    DRIVE_PICTURE_EDIT_CANCER("click34", "行车-图片-编辑-取消", "0"),
    DRIVE_PICTURE_SLIDE_CHANGE("click35", "行车-图片-滑动切换", "0"),
    DRIVE_PICTURE_LAST_PAGE("click36", "行车-图片-上一页", "0"),
    DRIVE_PICTURE_NEXT_PAGE("click37", "行车-图片-下一页", "0"),

    click39("click39", "导航-搜索", "0"),
    click40("click40", "导航-搜索中-取消", "0"),
    click41("click41", "导航-搜索结果列表项", "0"),
    click42("click42", "导航-搜索结果-取消", "0"),
    click43("click43", "蓝牙-拨号键盘", "0"),
    click44("click44", "蓝牙-通讯录", "0"),
    click45("click45", "蓝牙-拨打", "0"),
    click46("click46", "蓝牙-删除号码", "0"),
    click47("click47", "蓝牙-通讯录-搜索", "0"),
    click48("click48", "蓝牙-通讯录-搜索-删除", "0"),
    click49("click49", "蓝牙-通讯录列表项", "0"),
    click50("click50", "蓝牙-挂断", "0"),

    WIFI_HOT_OPEN("click51", "移动热点-打开", "0"),
    WIFI_HOT_CLOSE("click52", "移动热点-关闭", "0"),
    WIFI_HOT_PASSWORD_SET("click53", "移动热点-密码设置", "0"),
    WIFI_HOT_PASSWORD_CONFIRM("click54", "移动热点-密码-确认", "0"),
    WIFI_HOT_PASSWORD_CANCER("click55", "移动热点-密码-取消", "0"),

    CLICK_GUARD("click56", "防盗防劫-防盗", "0"),
    CLICK_UNLOCK_GUARD("click57", "防盗防劫-防盗-开", "0"),
    CLICK_LOCK_GUARD("click58", "防盗防劫-防盗-关", "0"),
    CLICK_ROBBERY("click59", "防盗防劫-防劫", "0"),
    CLICK_SWITCH_ON("click60", "防盗防劫-防劫-设置开", "0"),
    CLICK_SWITCH_OFF("click61", "防盗防劫-防劫-设置关", "0"),
    CLICK_UNLOCK_ROBBERY("click62", "防盗防劫-防劫-解锁", "0"),

    click63("click63", "VIP服务-挂断", "0"),

    voice1("voice1", " 车辆自检-打开车辆自检", "0"),
    voice2("voice2", " 车辆自检-退出自检", "0"),
    voice3("voice3", " 车辆自检-清除故障码", "0"),
    voice4("voice4", " 车辆自检-退出车辆自检", "0"),
    voice5("voice5", " 车辆自检-汽修店地址列表翻页", "0"),
    voice6("voice6", " 车辆自检-汽修店地址选择页数", "0"),
    voice7("voice7", " 车辆自检-汽修店地址选择", "0"),
    voice8("voice8", " 行车记录-打开前置录像预览", "0"),
    voice9("voice9", " 行车记录-退出录像预览", "0"),
    voice10("voice10", "行车记录-播放第几个录像", "0"),
    voice11("voice11", "行车记录-打开后置录像预览", "0"),
    voice12("voice12", "导航-打开", "0"),
    voice13("voice13", "导航-退出", "0"),
    voice14("voice14", "导航-当前位置播报", "0"),
    voice15("voice15", "导航-目的地导航", "0"),
    voice16("voice16", "导航-附近的xxx", "0"),
    voice17("voice17", "导航-最近的xxx", "0"),
    voice18("voice18", "导航-已经添加了家/公司/老家地址", "0"),
    voice19("voice19", "导航-没有添加家/公司/老家地址", "0"),
    voice20("voice20", "导航-修改家/公司/老家地址", "0"),
    voice21("voice21", "导航-附近的餐饮搜索", "0"),
    voice22("voice22", "导航-附近的川菜/粤菜/韩国菜", "0"),
    voice23("voice23", "导航-附近的酒店", "0"),
    voice24("voice24", "导航-附近的加油站", "0"),
    voice25("voice25", "导航-指定地名的位置", "0"),
    voice26("voice26", "导航-附近的电影院", "0"),
    voice27("voice27", "导航-附近的旅馆", "0"),
    voice28("voice28", "导航-导航目的地选择/导航路线优先策略", "0"),
    voice29("voice29", "导航-导航地址选择页数", "0"),
    voice30("voice30", "导航-导航地址翻页", "0"),
    voice31("voice31", "导航-导航路线优先策略选择", "0"),
    voice32("voice32", "导航-加完家/公司/老家地址后问是否导航", "0"),
    voice33("voice33", "天气-今天当前城市的天气", "0"),
    voice34("voice34", "天气-今天某城市的天气", "0"),
    voice35("voice35", "天气-某城市明天/后天的天气", "0"),
    voice36("voice36", "天气-明天/后天当前城市的天气", "0"),
    voice37("voice37", "天气-昨天天气", "0"),
    voice38("voice38", "天气-前天的天气", "0"),
    voice39("voice39", "天气-xx今天的天气", "0"),
    voice40("voice40", "天气-xx明天/后天的天气", "0"),
    voice41("voice41", "天气-当前定位城市明天/后天的天气", "0"),
    voice42("voice42", "音量调节-调低", "0"),
    voice43("voice43", "音量调节-关闭声音", "0"),
    voice44("voice44", "音量调节-打开音量", "0"),
    voice45("voice45", "音量调节-继续调节音量", "0"),
    voice46("voice46", "蓝牙电话-打电话给某个号码", "0"),
    voice47("voice47", "蓝牙电话-拨打某个号码", "0"),
    voice48("voice48", "蓝牙电话-给某个联系人拨打电话", "0"),
    voice49("voice49", "蓝牙电话-跳转至拨号界面", "0"),
    voice50("voice50", "蓝牙电话-跳转至通讯录页面", "0"),
    voice51("voice51", "蓝牙电话-一个联系人多个号码时", "0"),
    voice52("voice52", "防盗防劫-打开防劫页面", "0"),
    voice53("voice53", "防盗防劫-打开防盗页面", "0"),
    voice54("voice54", "防盗防劫-打开防劫页面并开启防劫", "0"),
    voice55("voice55", "防盗防劫-打开防盗页面并进行防盗解锁", "0"),
    voice56("voice56", "移动热点-打开WIFI热点", "0"),
    voice57("voice57", "移动热点-关闭WIFI热点", "0"),
    voice58("voice58", "移动热点-打开wifi页面", "0"),
    voice59("voice59", "亮度调节-调高", "0"),
    voice60("voice60", "亮度调节-调低", "0"),
    voice61("voice61", "VIP服务-拨打VIP电话", "0"),
    voice62("voice62", "屏幕-熄屏", "0"),
    voice63("voice63", "返回到首页", "0"),
    voice64("voice64", "退出语音助手", "0"),
    voice65("voice65", "退出到主页面", "0");




    private String eventId;
    private String eventName;
    private String eventType;


    ClickEvent(String eventId, String eventName, String eventType) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventType = eventType;
    }


    public String getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventType() {
        return eventType;
    }
}
