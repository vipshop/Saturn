package com.vip.saturn.job.integrate.service;

import com.vip.saturn.job.integrate.exception.ReportAlarmException;

import java.util.List;

/**
 * @author hebelala
 */
public interface ReportAlarmService {

    /**
     * The NamespaceShardingService execute allSharding error
     *
     * @param namespace The domain or namespace
     * @param hostValue The NamespaceShardingService thread leader's hostValue
     */
    void allShardingError(String namespace, String hostValue) throws ReportAlarmException;

    /**
     * Dashboard refresh data, find the container instances is mismatch
     *
     * @param namespace        The domain or namespace
     * @param taskId           The taskId of container source
     * @param configInstances  The instances configured
     * @param runningInstances The running instances
     */
    void dashboardContainerInstancesMismatch(String namespace, String taskId, int configInstances, int runningInstances) throws ReportAlarmException;

    /**
     * Dashboard refresh data, find the abnormal job
     *
     * @param namespace       The domain or namespace
     * @param jobName         The abnormal job's name
     * @param shouldFiredTime The time that job should be fired
     */
    void dashboardAbnormalJob(String namespace, String jobName, String shouldFiredTime) throws ReportAlarmException;

    /**
     * Dashboard refresh data, find that the job is timeout
     *
     * @param namespace            The domain or namespace
     * @param jobName              The timeout job's name
     * @param timeoutItems         The timeout items of the job
     * @param timeout4AlarmSeconds The timeout4AlarmSeconds of job configured
     */
    void dashboardTimeout4AlarmJob(String namespace, String jobName, List<Integer> timeoutItems, int timeout4AlarmSeconds) throws ReportAlarmException;

}
