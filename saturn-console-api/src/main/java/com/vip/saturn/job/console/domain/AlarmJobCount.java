/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

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
