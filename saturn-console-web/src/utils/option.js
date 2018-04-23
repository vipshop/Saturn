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
};
