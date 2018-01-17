<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <TimeoutJobs :timeout-jobs-list="timeoutJobsList" @refresh-list="getTimeoutJobs"></TimeoutJobs>
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
  },
  created() {
    this.getTimeoutJobs();
  },
};
</script>
