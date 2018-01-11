import Vue from 'vue';
import Router from 'vue-router';
import RouterMapper from './utils/router_mapper';
import JobManage from './pages/job_manage/job_manage';
import JobList from './pages/job_list/job_list';
import JobOverview from './pages/job_list/job_overview';
import ExecutorOverview from './pages/job_list/executor_overview';
import NamespaceAlarmCenter from './pages/job_list/alarm/namespace_alarm_center';
import NamespaceAbnormalJobs from './pages/job_list/alarm/namespace_abnormal_jobs';
import NamespaceTimeoutJobs from './pages/job_list/alarm/namespace_timeout_jobs';
import NamespaceFailoverJobs from './pages/job_list/alarm/namespace_failover_jobs';
import NamespaceAbnormalContainers from './pages/job_list/alarm/namespace_abnormal_containers';
import JobDetail from './pages/job_detail/job_detail';
import JobSetting from './pages/job_detail/job_setting';
import JobSharding from './pages/job_detail/job_sharding';
import JobExecution from './pages/job_detail/job_execution';
import JobStatistics from './pages/job_detail/job_statistics';
import JobAlarmCenter from './pages/job_detail/alarm/job_alarm_center';
import JobAbnormalJobs from './pages/job_detail/alarm/job_abnormal_jobs';
import JobTimeoutJobs from './pages/job_detail/alarm/job_timeout_jobs';
import JobFailoverJobs from './pages/job_detail/alarm/job_failover_jobs';
import DashboardManage from './pages/dashboard_manage/dashboard_manage';
import DashboardDetail from './pages/dashboard_manage/dashboard_detail';
import DomainStatistic from './pages/dashboard_manage/domain_statistic';
import ExecutorStatistic from './pages/dashboard_manage/executor_statistic';
import JobStatistic from './pages/dashboard_manage/job_statistic';
import RegistryManage from './pages/registry_manage/registry_manage';
import AlarmManage from './pages/alarm_manage/alarm_manage';
import AlarmAbnormalJobs from './pages/alarm_manage/alarm_abnormal_jobs';
import AlarmTimeoutJobs from './pages/alarm_manage/alarm_timeout_jobs';
import UnableFailoverJobs from './pages/alarm_manage/unable_failover_jobs';
import AlarmAbnormalContainers from './pages/alarm_manage/alarm_abnormal_containers';

Vue.use(Router);

export default new Router({
  routes: [
    {
      path: RouterMapper.GetPath('jobManage'),
      name: 'job_manage',
      component: JobManage,
    }, {
      path: '/',
      redirect: 'job_manage',
    }, {
      path: RouterMapper.GetPath('jobList'),
      component: JobList,
      children: [
        { path: '', redirect: 'job_overview' },
        { name: 'job_overview', path: RouterMapper.GetPath('jobOverview'), component: JobOverview },
        { name: 'executor_overview', path: RouterMapper.GetPath('executorOverview'), component: ExecutorOverview },
        {
          path: RouterMapper.GetPath('namespaceAlarmCenter'),
          component: NamespaceAlarmCenter,
          name: 'namespace_alarm_center',
          children: [
            { path: RouterMapper.GetPath('namespaceAlarmCenter'), redirect: 'namespace_abnormal_jobs' },
            { name: 'namespace_abnormal_jobs', path: RouterMapper.GetPath('namespaceAbnormalJobs'), component: NamespaceAbnormalJobs },
            { name: 'namespace_timeout_jobs', path: RouterMapper.GetPath('namespaceTimeoutJobs'), component: NamespaceTimeoutJobs },
            { name: 'namespace_failover_jobs', path: RouterMapper.GetPath('namespaceFailoverJobs'), component: NamespaceFailoverJobs },
            { name: 'namespace_abnormal_containers', path: RouterMapper.GetPath('namespaceAbnormalContainers'), component: NamespaceAbnormalContainers },
          ],
        },
      ],
    }, {
      path: RouterMapper.GetPath('jobDetail'),
      component: JobDetail,
      children: [
        { path: '', redirect: 'job_setting' },
        { name: 'job_setting', path: RouterMapper.GetPath('jobSetting'), component: JobSetting },
        { name: 'job_sharding', path: RouterMapper.GetPath('jobSharding'), component: JobSharding },
        { name: 'job_execution', path: RouterMapper.GetPath('jobExecution'), component: JobExecution },
        { name: 'job_statistics', path: RouterMapper.GetPath('jobStatistics'), component: JobStatistics },
        {
          path: RouterMapper.GetPath('jobAlarmCenter'),
          component: JobAlarmCenter,
          name: 'job_alarm_center',
          children: [
            { path: RouterMapper.GetPath('jobAlarmCenter'), redirect: 'job_abnormal_jobs' },
            { name: 'job_abnormal_jobs', path: RouterMapper.GetPath('jobAbnormalJobs'), component: JobAbnormalJobs },
            { name: 'job_timeout_jobs', path: RouterMapper.GetPath('jobTimeoutJobs'), component: JobTimeoutJobs },
            { name: 'job_failover_jobs', path: RouterMapper.GetPath('jobFailoverJobs'), component: JobFailoverJobs },
          ],
        },
      ],
    }, {
      path: RouterMapper.GetPath('dashboardManage'),
      name: 'dashboard_manage',
      component: DashboardManage,
    }, {
      path: RouterMapper.GetPath('dashboardDetail'),
      component: DashboardDetail,
      children: [
        { path: '', redirect: 'domain_statistic' },
        { name: 'domain_statistic', path: RouterMapper.GetPath('domainStatistic'), component: DomainStatistic },
        { name: 'executor_statistic', path: RouterMapper.GetPath('executorStatistic'), component: ExecutorStatistic },
        { name: 'job_statistic', path: RouterMapper.GetPath('jobStatistic'), component: JobStatistic },
      ],
    }, {
      path: RouterMapper.GetPath('registryManage'),
      name: 'registry_manage',
      component: RegistryManage,
    }, {
      path: RouterMapper.GetPath('alarmManage'),
      component: AlarmManage,
      children: [
        { path: '', redirect: 'alarm_abnormal_jobs' },
        { name: 'alarm_abnormal_jobs', path: RouterMapper.GetPath('alarmAbnormalJobs'), component: AlarmAbnormalJobs },
        { name: 'alarm_timeout_jobs', path: RouterMapper.GetPath('alarmTimeoutJobs'), component: AlarmTimeoutJobs },
        { name: 'unable_failover_jobs', path: RouterMapper.GetPath('unableFailoverJobs'), component: UnableFailoverJobs },
        { name: 'alarm_abnormal_containers', path: RouterMapper.GetPath('alarmAbnormalContainers'), component: AlarmAbnormalContainers },
      ],
    },
  ],
});
