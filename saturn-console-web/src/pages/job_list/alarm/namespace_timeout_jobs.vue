<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <TimeoutJobs :timeout-jobs-list="namespaceTimeoutJobsList" @refresh-list="getTimeoutJobs"></TimeoutJobs>
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
        this.total = this.namespaceTimeoutJobsList.length;
      })
      .catch(() => { this.$http.buildErrorHandler('获取异常作业请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
  },
  created() {
    this.getTimeoutJobs();
  },
};
</script>
