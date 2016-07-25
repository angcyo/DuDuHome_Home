package com.dudu.workflow.push.model;

/**
 * Created by Administrator on 2016/3/30.
 */
public class PushParams {

    /**
     * 防盗开启状态
     */
    public static final String GUARD_STATE = "theft_set_state";

    /**
     * 加速测试开始
     */
    public static final String TEST_SPEED_START = "test_speed_start";

    /**
     * 加速测试停止
     * */

    public static final String TEST_SPEED_STOP = "test_speed_stop";

    /**
     * 防劫开启的状态
     */
    public static final String ROBBERY_STATE = "robbery_state";

    /**
     * 防盗密码的设置
     */
    public static final String GUARD_SET_PASSWORD = "theft_set_password";

    /**
     * 防盗审核状态
     */
    public static final String THEFT_APPROVAL = "theft_approval";


    /**
     * 日志上传
     */
    public static final String LOG_UPLOAD = "logs";

    /**
     * launcher升级
     */
    public static final String LAUNCHER_UPGRADE = "upgrade";

    /**
     * 流量充值
     */
    public static final String TRAFFIC_RECHARGE = "traffic_recharge";

    /**
     * 开始监控
     */
    public static final String START_STREAM = "start_stream";

    /**
     * 停止监控
     */
    public static final String STOP_STREAM = "stop_stream";

    /**
     * 实时监控的命令参数
     */
    public static final String VIDEO_STREAM = "smedia_request";
    /**
     * 设置胎压告警阈值
     */
    public static final String SET_TIRE_PRESSURE = "tire_pressure_set";
}
