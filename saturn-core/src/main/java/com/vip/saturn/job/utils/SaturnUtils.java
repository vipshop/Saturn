package com.vip.saturn.job.utils;

import com.vip.saturn.job.internal.config.JobConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SaturnUtils {

    public static String convertTime2FormattedString (long time){
        Date date = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        return sdf.format(date);
    }


    /**
     * 如果存在/config/enabledReport节点，则返回节点的内容；
     * 如果不存在/config/enabledReport节点，如果作业类型是Java或者Shell，则返回true；否则，返回false；
     */
    public static boolean checkIfJobIsEnabledReport(JobConfiguration jobConfiguration) {
        if (jobConfiguration == null) {
            throw new RuntimeException("JobConfiguration cannot be null");
        }

        Boolean isEnabledReportInJobConfig = jobConfiguration.isEnabledReport();

        if (isEnabledReportInJobConfig != null) {
            return isEnabledReportInJobConfig;
        }
        // if isEnabledReportInJobConfig == null, 如果作业类型是JAVA或者Shell，默认上报
        if ("JAVA_JOB".equals(jobConfiguration.getJobType())
                || "SHELL_JOB".equals(jobConfiguration.getJobType())) {
            return true;
        }

        return false;
    }
}
