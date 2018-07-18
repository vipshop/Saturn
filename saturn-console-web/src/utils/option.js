export default {
  jobTypes: [{
    value: 'JAVA_JOB',
    label: 'Java定时作业',
  }, {
    value: 'SHELL_JOB',
    label: 'Shell定时作业',
  }],
  jobStatusTypes: [{
    value: 0,
    label: '已停止',
  }, {
    value: 1,
    label: '已就绪',
  }, {
    value: 2,
    label: '运行中',
  }, {
    value: 3,
    label: '停止中',
  }],
};
