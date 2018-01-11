<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <TimeoutJobs :timeout-jobs-list="jobTimeoutJobsList" @refresh-list="getTimeoutJobs"></TimeoutJobs>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      domainName: this.$route.params.domain,
      jobName: this.$route.params.jobName,
      loading: false,
      jobTimeoutJobsList: [],
    };
  },
  methods: {
    getTimeoutJobs() {
      this.loading = true;
      this.$http.get(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/alarmStatistics/timeout4AlarmJob`).then((data) => {
        this.jobTimeoutJobsList = JSON.parse(data);
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
