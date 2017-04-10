package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.integrate.exception.ReportAlarmException;
import com.vip.saturn.job.integrate.service.ReportAlarmService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author hebelala
 */
@Service
public class ReportAlarmServiceImpl implements ReportAlarmService {
	
    public enum EventType {

        UnnormalJob("UnnormalJob"),
        CONTAINER_INSTANCE_MISMATCH("CONTAINER.INSTANCE.MISMATCH"),
        SHARDING_ALLSHARDING_EXCEPTION("SHARDING.ALLSHARDING.EXCEPTION");
        private String type;

        EventType(String type) {
            this.type = type;
        }

        public String getValue() {
            return type;
        }

        public static EventType getByName(String name) {
            if(name != null) {
                EventType[] values = EventType.values();
                if (values != null && values.length > 0) {
                    for (EventType eventType : values) {
                        if (name.equals(eventType.name())) {
                            return eventType;
                        }
                    }
                }
            }
            return null;
        }
    }	


    @Override
    public void allShardingError(String namespace, String hostValue) throws ReportAlarmException {

    }

    @Override
    public void dashboardContainerInstancesMismatch(String namespace, String taskId, int configInstances, int runningInstances) throws ReportAlarmException {

    }

    @Override
    public void dashboardAbnormalJob(String namespace, String jobName, String shouldFiredTime) throws ReportAlarmException {

    }

    @Override
    public void dashboardTimeout4AlarmJob(String namespace, String jobName, List<Integer> timeoutItems, int timeout4AlarmSeconds) throws ReportAlarmException {

    }
}
