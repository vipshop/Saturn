package com.vip.saturn.job.console.vo;

/**
 * @author hebelala
 */
public enum JobType {

    JAVA_JOB, SHELL_JOB, UNKOWN_JOB;

    public static final JobType getJobType(String jobType) {
        try {
            return valueOf(jobType);
        } catch (Exception e) {
            return UNKOWN_JOB;
        }
    }

}
