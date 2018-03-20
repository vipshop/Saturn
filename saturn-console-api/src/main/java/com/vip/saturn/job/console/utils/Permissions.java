package com.vip.saturn.job.console.utils;

import com.vip.saturn.job.console.mybatis.entity.Permission;

/**
 * @author hebelala
 */
public class Permissions {

	public static final Permission jobEnable = new Permission("job:enable");
	public static final Permission jobBatchEnable = new Permission("job:batchEnable");
	public static final Permission jobDisable = new Permission("job:disable");
	public static final Permission jobBatchDisable = new Permission("job:batchDisable");
	public static final Permission jobRunAtOnce = new Permission("job:runAtOnce");
	public static final Permission jobStopAtOnce = new Permission("job:stopAtOnce");
	public static final Permission jobRemove = new Permission("job:remove");
	public static final Permission jobBatchRemove = new Permission("job:batchRemove");
	public static final Permission jobAdd = new Permission("job:add");
	public static final Permission jobCopy = new Permission("job:copy");
	public static final Permission jobImport = new Permission("job:import");
	public static final Permission jobExport = new Permission("job:export");
	public static final Permission jobUpdate = new Permission("job:update");
	public static final Permission jobBatchSetPreferExecutors = new Permission("job:batchSetPreferExecutors");

	public static final Permission executorRestart = new Permission("executor:restart");
	public static final Permission executorDump = new Permission("executor:dump");
	public static final Permission executorExtractOrRecoverTraffic = new Permission("executor:extractOrRecoverTraffic");
	public static final Permission executorBatchExtractOrRecoverTraffic = new Permission(
			"executor:batchExtractOrRecoverTraffic");
	public static final Permission executorRemove = new Permission("executor:remove");
	public static final Permission executorBatchRemove = new Permission("executor:batchRemove");
	public static final Permission executorShardAllAtOnce = new Permission("executor:shardAllAtOnce");

	public static final Permission alarmCenterSetAbnormalJobRead = new Permission("alarmCenter:setAbnormalJobRead");
	public static final Permission alarmCenterSetTimeout4AlarmJobRead = new Permission(
			"alarmCenter:setTimeout4AlarmJobRead");

	public static final Permission dashboardCleanShardingCount = new Permission("dashboard:cleanShardingCount");
	public static final Permission dashboardCleanOneJobAnalyse = new Permission("dashboard:cleanOneJobAnalyse");
	public static final Permission dashboardCleanAllJobAnalyse = new Permission("dashboard:cleanAllJobAnalyse");
	public static final Permission dashboardCleanOneJobExecutorCount = new Permission(
			"dashboard:cleanOneJobExecutorCount");

	public static final Permission registryCenterAddNamespace = new Permission("registryCenter:addNamespace");
	public static final Permission registryCenterBatchMoveNamespaces = new Permission(
			"registryCenter:batchMoveNamespaces");
	public static final Permission registryCenterExportNamespaces = new Permission("registryCenter:exportNamespaces");
	public static final Permission registryCenterAddZkCluster = new Permission("registryCenter:addZkCluster");

	public static final Permission systemConfig = new Permission("systemConfig");
	
	public static final Permission authorizationManage = new Permission("authorizationManage");

}
