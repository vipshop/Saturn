package com.vip.saturn.job.console.domain;

import java.io.Serializable;

/**
 * Created by ivy01.li on 2018/3/9.
 */
public class AlarmJobCount implements Serializable {
    private static final long serialVersionUID = 1L;

    private String alarmJobType;
    private int count;

    public AlarmJobCount() {}

    public AlarmJobCount(String alarmJobType, int count) {
        this.alarmJobType = alarmJobType;
        this.count = count;
    }

    public String getAlarmJobType() {
        return alarmJobType;
    }

    public int getCount() {
        return count;
    }

    public void setAlarmJobType(String alarmJobType) {
        this.alarmJobType = alarmJobType;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
