<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <TimeoutJobs :timeout-jobs-list="namespaceTimeoutJobsList" @refresh-list="getTimeoutJobs" @no-alarm="noAlarm"></TimeoutJobs>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      domainName: this.$route.params.domain,
      loading: false,
      namespaceTimeoutJobsList: [],
    };
  },
  methods: {
    getTimeoutJobs() {
      this.loading = true;
      this.$http.get(`/console/namespaces/${this.domainName}/alarmStatistics/timeout4AlarmJobs`).then((data) => {
        this.namespaceTimeoutJobsList = JSON.parse(data);
      })
      .catch(() => { this.$http.buildErrorHandler('获取异常作业请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    noAlarm(params) {
      this.$http.post(`/console/namespaces/${params.domainName}/setTimeout4AlarmJobMonitorStatusToRead`, { uuid: params.uuid }).then(() => {
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
