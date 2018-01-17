<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <UnableFailoverJobs :unable-failover-jobs-list="unableFailoverJobsList" @refresh-list="getUnableFailoverJobs"></UnableFailoverJobs>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      unableFailoverJobsList: [],
    };
  },
  methods: {
    getUnableFailoverJobs() {
      this.loading = true;
      this.$http.get('/console/alarmStatistics/unableFailoverJobs').then((data) => {
        this.unableFailoverJobsList = JSON.parse(data);
      })
      .catch(() => { this.$http.buildErrorHandler('获取异常作业请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
  },
  created() {
    this.getUnableFailoverJobs();
  },
};
</script>
