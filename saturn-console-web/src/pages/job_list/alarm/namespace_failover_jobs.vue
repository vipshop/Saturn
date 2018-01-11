<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <UnableFailoverJobs :unable-failover-jobs-list="namespaceUnableFailoverJobsList" @refresh-list="getUnableFailoverJobs"></UnableFailoverJobs>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      domainName: this.$route.params.domain,
      namespaceUnableFailoverJobsList: [],
      filters: {
        jobName: '',
      },
      orderBy: 'jobName',
      total: 0,
    };
  },
  methods: {
    getUnableFailoverJobs() {
      this.loading = true;
      this.$http.get(`/console/namespaces/${this.domainName}/alarmStatistics/unableFailoverJobs`).then((data) => {
        this.namespaceUnableFailoverJobsList = JSON.parse(data);
        this.total = this.namespaceUnableFailoverJobsList.length;
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
