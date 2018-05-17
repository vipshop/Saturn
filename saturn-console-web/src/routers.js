import Vue from 'vue';
import Router from 'vue-router';
import RouterMapper from './utils/router_mapper';
import Login from './Login';
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
import JobAlarm from './pages/job_detail/job_alarm';
import DashboardManage from './pages/dashboard_manage/dashboard_manage';
import DashboardDetail from './pages/dashboard_manage/dashboard_detail';
import DomainStatistic from './pages/dashboard_manage/domain_statistic';
import ExecutorStatistic from './pages/dashboard_manage/executor_statistic';
import JobStatistic from './pages/dashboard_manage/job_statistic';
import RegistryManage from './pages/registry_manage/registry_manage';
import ClustersManage from './pages/registry_manage/clusters_manage';
import NamespaceManage from './pages/registry_manage/namespace_manage';
import AlarmManage from './pages/alarm_manage/alarm_manage';
import AlarmAbnormalJobs from './pages/alarm_manage/alarm_abnormal_jobs';
import AlarmTimeoutJobs from './pages/alarm_manage/alarm_timeout_jobs';
import UnableFailoverJobs from './pages/alarm_manage/unable_failover_jobs';
import AlarmAbnormalContainers from './pages/alarm_manage/alarm_abnormal_containers';
import SystemConfigManage from './pages/system_config_manage/system_config_manage';
import ConsoleConfig from './pages/system_config_manage/console_config';
import AuthorityManage from './pages/authority_manage/authority_manage';

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
      path: RouterMapper.GetPath('login'),
      name: 'login',
      component: Login,
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
        { name: 'job_alarm', path: RouterMapper.GetPath('jobAlarm'), component: JobAlarm },
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
      component: RegistryManage,
      children: [
        { path: '', redirect: 'namespace_manage' },
        { name: 'namespace_manage', path: RouterMapper.GetPath('namespaceManage'), component: NamespaceManage },
        { name: 'clusters_manage', path: RouterMapper.GetPath('clustersManage'), component: ClustersManage },
      ],
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
    }, {
      path: RouterMapper.GetPath('systemConfigManage'),
      component: SystemConfigManage,
      children: [
        { path: '', redirect: 'console_config' },
        { name: 'console_config', path: RouterMapper.GetPath('consoleConfig'), component: ConsoleConfig },
      ],
    }, {
      path: RouterMapper.GetPath('authorityManage'),
      name: 'authority_manage',
      component: AuthorityManage,
    },
  ],
});
