package com.dudu.rest.model;

import com.dudu.rest.model.common.RequestResponse;

/**
 * Created by Administrator on 2016/5/21.
 */
public class RobberySwitchStatusResponse extends RequestResponse{
    public Result result;

    public class Result {
        public String audit_state;

        public String robbery_open_time;

        public String protect_rob_state;

        public String insurance_url;

        @Override
        public String toString() {
            return "Result{" +
                    "audit_state='" + audit_state + '\'' +
                    ", robbery_open_time='" + robbery_open_time + '\'' +
                    ", protect_rob_state='" + protect_rob_state + '\'' +
                    ", insurance_url='" + insurance_url + '\'' +
                    '}';
        }
    }

}
