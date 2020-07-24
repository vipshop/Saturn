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

package com.vip.saturn.job.console.utils;

/**
 * @author hebelala
 */
public class PermissionKeys {

	public static final String jobEnable = "job:enable";
	public static final String jobBatchEnable = "job:batchEnable";
	public static final String jobDisable = "job:disable";
	public static final String jobBatchDisable = "job:batchDisable";
	public static final String jobRunAtOnce = "job:runAtOnce";
	public static final String jobBatchRunAtOnce = "job:batchRunAtOnce";
	public static final String jobStopAtOnce = "job:stopAtOnce";
	public static final String jobRemove = "job:remove";
	public static final String jobBatchRemove = "job:batchRemove";
	public static final String jobAdd = "job:add";
	public static final String jobCopy = "job:copy";
	public static final String jobImport = "job:import";
	public static final String jobExport = "job:export";
	public static final String jobUpdate = "job:update";
	public static final String jobBatchSetPreferExecutors = "job:batchSetPreferExecutors";

	public static final String executorRestart = "executor:restart";
	public static final String executorDump = "executor:dump";
	public static final String executorExtractOrRecoverTraffic = "executor:extractOrRecoverTraffic";
	public static final String executorBatchExtractOrRecoverTraffic = "executor:batchExtractOrRecoverTraffic";
	public static final String executorRemove = "executor:remove";
	public static final String executorBatchRemove = "executor:batchRemove";
	public static final String executorShardAllAtOnce = "executor:shardAllAtOnce";

	public static final String alarmCenterSetAbnormalJobRead = "alarmCenter:setAbnormalJobRead";
	public static final String alarmCenterSetTimeout4AlarmJobRead = "alarmCenter:setTimeout4AlarmJobRead";

	public static final String dashboardCleanShardingCount = "dashboard:cleanShardingCount";
	public static final String dashboardCleanOneJobAnalyse = "dashboard:cleanOneJobAnalyse";
	public static final String dashboardCleanAllJobAnalyse = "dashboard:cleanAllJobAnalyse";
	public static final String dashboardCleanOneJobExecutorCount = "dashboard:cleanOneJobExecutorCount";

	public static final String registryCenterAddNamespace = "registryCenter:addNamespace";
	public static final String registryCenterBatchMoveNamespaces = "registryCenter:batchMoveNamespaces";
	public static final String registryCenterExportNamespaces = "registryCenter:exportNamespaces";
	public static final String registryCenterAddZkCluster = "registryCenter:addZkCluster";
	public static final String registryCenterRemoveZkCluster = "registryCenter:removeZkCluster";

	public static final String systemConfig = "systemConfig";

	public static final String authorizationManage = "authorizationManage";

}
