<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <AbnormalJobs :abnormal-jobs-list="namespaceAbnormalJobsList" @refresh-list="getAbnormalJobs" @no-alarm="noAlarm"></AbnormalJobs>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      domainName: this.$route.params.domain,
      namespaceAbnormalJobsList: [],
    };
  },
  methods: {
    getAbnormalJobs() {
      this.loading = true;
      this.$http.get(`/console/namespaces/${this.domainName}/alarmStatistics/abnormalJobs`).then((data) => {
        this.namespaceAbnormalJobsList = JSON.parse(data);
      })
      .catch(() => { this.$http.buildErrorHandler('获取异常作业请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    noAlarm(params) {
      this.$http.post(`/console/namespaces/${params.domainName}/setAbnormalJobMonitorStatusToRead`, { uuid: params.uuid }).then(() => {
        this.getAbnormalJobs();
        this.$message.successNotify('操作成功');
      })
      .catch(() => { this.$http.buildErrorHandler('不再告警操作请求失败！'); });
    },
  },
  created() {
    this.getAbnormalJobs();
  },
};
</script>
