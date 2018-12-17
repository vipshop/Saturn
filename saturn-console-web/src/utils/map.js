export default {
  degreeMap: {
    '': '没有定义',
    0: '没有定义',
    1: '非线上业务',
    2: '简单业务',
    3: '一般业务',
    4: '重要业务',
    5: '核心业务',
  },
  causeMap: {
    NOT_RUN: '过时未跑',
    NO_SHARDS: '没有分片',
    EXECUTORS_NOT_READY: '没有executor能运行该作业',
  },
  jobTypeMap: {
    JAVA_JOB: 'JAVA定时',
    SHELL_JOB: 'SHELL定时',
    PASSIVE_JAVA_JOB: 'JAVA被动',
    PASSIVE_SHELL_JOB: 'SHELL被动',
  },
  jobStatusMap: {
    READY: '已就绪',
    RUNNING: '运行中',
    STOPPING: '停止中',
    STOPPED: '已停止',
  },
};
