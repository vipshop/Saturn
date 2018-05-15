export default {
  jobTypes: [{
    value: 'JAVA_JOB',
    label: 'Java定时作业',
  }, {
    value: 'SHELL_JOB',
    label: 'Shell定时作业',
  }],
  jobStatusTypes: [{
    value: 'READY',
    label: '已就绪',
  }, {
    value: 'RUNNING',
    label: '运行中',
  }, {
    value: 'STOPPING',
    label: '停止中',
  }, {
    value: 'STOPPED',
    label: '已停止',
  }],
  roleTypes: [{
    value: 'system_admin',
    label: '系统管理员',
  }, {
    value: 'namespace_admin',
    label: '域管理员',
  }, {
    value: 'namespace_developer',
    label: '域开发人员',
  }],
};
