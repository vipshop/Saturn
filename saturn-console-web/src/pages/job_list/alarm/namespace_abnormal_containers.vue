<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <AbnormalContainers :abnormal-containers-list="namespaceAbnormalContainersList" @refresh-list="getAbnormalContainers"></AbnormalContainers>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      domainName: this.$route.params.domain,
      namespaceAbnormalContainersList: [],
    };
  },
  methods: {
    getAbnormalContainers() {
      this.loading = true;
      this.$http.get(`/console/namespaces/${this.domainName}/alarmStatistics/abnormalContainers`).then((data) => {
        this.namespaceAbnormalContainersList = JSON.parse(data);
      })
      .catch(() => { this.$http.buildErrorHandler('获取异常作业请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
  },
  created() {
    this.getAbnormalContainers();
  },
};
</script>
