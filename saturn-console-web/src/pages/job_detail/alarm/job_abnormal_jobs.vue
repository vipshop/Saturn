<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <AbnormalJobs :abnormal-jobs-list="jobAbnormalJobsList" @refresh-list="getAbnormalJobs"></AbnormalJobs>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      domainName: this.$route.params.domain,
      jobName: this.$route.params.jobName,
      jobAbnormalJobsList: [],
    };
  },
  methods: {
    getAbnormalJobs() {
      this.loading = true;
      this.$http.get(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/alarmStatistics/abnormalJobs`).then((data) => {
        this.jobAbnormalJobsList = JSON.parse(data);
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
