<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <TimeoutJobs :timeout-jobs-list="timeoutJobsList" @refresh-list="getTimeoutJobs" @no-alarm="noAlarm"></TimeoutJobs>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      timeoutJobsList: [],
    };
  },
  methods: {
    getTimeoutJobs() {
      this.loading = true;
      this.$http.get('/console/alarmStatistics/timeout4AlarmJobs').then((data) => {
        this.timeoutJobsList = JSON.parse(data);
      })
      .catch(() => { this.$http.buildErrorHandler('获取异常作业请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    noAlarm(uuid) {
      this.$http.post('/console/alarmStatistics/setTimeout4AlarmJobMonitorStatusToRead', { uuid }).then(() => {
        this.getTimeoutJobs();
        this.$message.successNotify('操作成功');
      })
      .catch(() => { this.$http.buildErrorHandler('不再告警操作请求失败！'); });
    },
  },
  created() {
    this.getTimeoutJobs();
  },
};
</script>
