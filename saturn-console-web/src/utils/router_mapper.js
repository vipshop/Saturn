const routermapperlist = [
    { name: 'login', path: '/login' },
    { name: 'jobManage', path: '/job_manage' },
    { name: 'jobList', path: '/job_list/:domain' },
    { name: 'jobOverview', path: '/job_list/:domain/job_overview' },
    { name: 'executorOverview', path: '/job_list/:domain/executor_overview' },
    { name: 'namespaceAlarmCenter', path: '/job_list/:domain/namespace_alarm_center' },
    { name: 'namespaceAbnormalJobs', path: '/job_list/:domain/namespace_alarm_center/namespace_abnormal_jobs' },
    { name: 'namespaceTimeoutJobs', path: '/job_list/:domain/namespace_alarm_center/namespace_timeout_jobs' },
    { name: 'namespaceFailoverJobs', path: '/job_list/:domain/namespace_alarm_center/namespace_failover_jobs' },
    { name: 'namespaceAbnormalContainers', path: '/job_list/:domain/namespace_alarm_center/namespace_abnormal_containers' },
    { name: 'jobDetail', path: '/job_detail/:domain/:jobName' },
    { name: 'jobSetting', path: '/job_detail/:domain/:jobName/job_setting' },
    { name: 'jobSharding', path: '/job_detail/:domain/:jobName/job_sharding' },
    { name: 'jobExecution', path: '/job_detail/:domain/:jobName/job_execution' },
    { name: 'jobAlarm', path: '/job_detail/:domain/:jobName/job_alarm' },
    { name: 'dashboardManage', path: '/dashboard_manage' },
    { name: 'dashboardDetail', path: '/dashboard_detail' },
    { name: 'domainStatistic', path: '/dashboard_detail/domain_statistic' },
    { name: 'executorStatistic', path: '/dashboard_detail/executor_statistic' },
    { name: 'jobStatistic', path: '/dashboard_detail/job_statistic' },
    { name: 'alarmManage', path: '/alarm_manage' },
    { name: 'alarmAbnormalJobs', path: '/alarm_manage/alarm_abnormal_jobs' },
    { name: 'alarmTimeoutJobs', path: '/alarm_manage/alarm_timeout_jobs' },
    { name: 'unableFailoverJobs', path: '/alarm_manage/unable_failover_jobs' },
    { name: 'alarmAbnormalContainers', path: '/alarm_manage/alarm_abnormal_containers' },
    { name: 'registryManage', path: '/registry_manage' },
    { name: 'namespaceManage', path: '/registry_manage/namespace_manage' },
    { name: 'clustersManage', path: '/registry_manage/clusters_manage' },
    { name: 'systemConfigManage', path: '/system_config_manage' },
    { name: 'consoleConfig', path: '/system_config_manage/console_config' },
    { name: 'authorityManage', path: '/authority_manage' },
];

export default {
  GetPath(name) {
    const items = routermapperlist.filter(x => x.name === name);
    if (items.length > 0) {
      return items[0].path;
    }
    throw new Error('can not find route path ');
  },
};
