package com.lsx.core.common.constant;

public class MqConstants {

    /**
     * 报修业务交换机
     */
    public static final String REPAIR_EXCHANGE = "smart.repair.exchange";

    /**
     * 报修提交队列
     */
    public static final String REPAIR_SUBMIT_QUEUE = "smart.repair.submit.queue";

    /**
     * 报修提交路由键
     */
    public static final String REPAIR_SUBMIT_ROUTING_KEY = "repair.submit";

    /**
     * 操作日志交换机
     */
    public static final String OPER_LOG_EXCHANGE = "smart.operlog.exchange";

    /**
     * 操作日志队列
     */
    public static final String OPER_LOG_QUEUE = "smart.operlog.queue";

    /**
     * 操作日志路由键
     */
    public static final String OPER_LOG_ROUTING_KEY = "operlog.add";
    /**
     * 活动交换机
     */
    public static final String ACTIVITY_EXCHANGE = "smart.activity.exchange";

    /**
     * 活动报名队列
     */
    public static final String ACTIVITY_SIGNUP_QUEUE = "smart.activity.signup.queue";

    /**
     * 活动报名路由键
     */
    public static final String ACTIVITY_SIGNUP_ROUTING_KEY = "activity.signup";
}
