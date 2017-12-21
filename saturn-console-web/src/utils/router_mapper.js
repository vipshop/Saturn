const routermapperlist = [
    { name: 'jobManage', path: '/job_manage' },
    { name: 'jobList', path: '/job_list/:domain' },
    { name: 'jobOverview', path: '/job_list/:domain/job_overview' },
    { name: 'executorOverview', path: '/job_list/:domain/executor_overview' },
    { name: 'jobDetail', path: '/job_detail/:domain/:jobName' },
    { name: 'jobSetting', path: '/job_detail/:domain/:jobName/job_overview/job_setting' },
    { name: 'jobExecutor', path: '/job_detail/:domain/:jobName/job_overview/job_executor' },
    { name: 'dashboardManage', path: '/dashboard_manage' },
    { name: 'registryManage', path: '/registry_manage' },
];

export default {
  GetPath(name) {
    const items = routermapperlist.filter(x => x.name === name);
    if (items !== undefined) {
      return items[0].path;
    }
    throw new Error('can not find route path ');
  },
};
