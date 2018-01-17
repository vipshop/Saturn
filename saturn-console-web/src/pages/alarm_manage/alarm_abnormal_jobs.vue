<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <AbnormalJobs :abnormal-jobs-list="abnormalJobsList" @refresh-list="getAbnormalJobs"></AbnormalJobs>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      abnormalJobsList: [],
    };
  },
  methods: {
    getAbnormalJobs() {
      this.loading = true;
      this.$http.get('/console/alarmStatistics/abnormalJobs').then((data) => {
        this.abnormalJobsList = JSON.parse(data);
      })
      .catch(() => { this.$http.buildErrorHandler('获取异常作业请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
  },
  created() {
    this.getAbnormalJobs();
  },
};
</script>
