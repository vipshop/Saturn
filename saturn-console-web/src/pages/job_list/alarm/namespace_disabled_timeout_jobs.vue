<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <TimeoutJobs :timeout-jobs-list="dataList" @refresh-list="getDisabledTimeoutJobs" @no-alarm="noAlarm" type="disabledTimeout"></TimeoutJobs>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      dataList: [],
    };
  },
  computed: {
    domainName() {
      return this.$route.params.domain;
    },
  },
  methods: {
    getDisabledTimeoutJobs() {
      this.loading = true;
      this.$http.get(`/console/namespaces/${this.domainName}/alarmStatistics/disabledTimeoutJobs`).then((data) => {
        this.dataList = JSON.parse(data);
      })
      .catch(() => { this.$http.buildErrorHandler('获取禁用超时作业请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    noAlarm(params) {
      this.$http.post(`/console/namespaces/${params.domainName}/setDisabledTimeoutJobMonitorStatusToRead`, { uuid: params.uuid }).then(() => {
        this.getDisabledTimeoutJobs();
        this.$message.successNotify('操作成功');
      })
      .catch(() => { this.$http.buildErrorHandler('不再告警操作请求失败！'); });
    },
  },
  created() {
    this.getDisabledTimeoutJobs();
  },
};
</script>
