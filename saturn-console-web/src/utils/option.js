export default {
  jobTypes: [{
    value: 'JAVA_JOB',
    label: 'Java定时作业',
  }, {
    value: 'SHELL_JOB',
    label: 'Shell定时作业',
  }, {
    value: 'PASSIVE_JAVA_JOB',
    label: 'Java被动作业',
  }, {
    value: 'PASSIVE_SHELL_JOB',
    label: 'Shell被动作业',
  }],
  isJava(jobType) {
    return jobType === this.jobTypes[0].value || jobType === this.jobTypes[2].value || jobType === 'MSG_JOB';
  },
  isShell(jobType) {
    return jobType === this.jobTypes[1].value || jobType === this.jobTypes[3].value || jobType === 'VSHELL';
  },
  isCron(jobType) {
    return jobType === this.jobTypes[0].value || jobType === this.jobTypes[1].value;
  },
  isPassive(jobType) {
    return jobType === this.jobTypes[2].value || jobType === this.jobTypes[3].value;
  },
  isMsg(jobType) {
    return jobType === 'MSG_JOB' || jobType === 'VSHELL';
  },
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
