<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <AbnormalContainers @abnormal-containers-list="abnormalContainersList" @refresh-list="getAbnormalContainers"></AbnormalContainers>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      abnormalContainersList: [],
    };
  },
  methods: {
    getAbnormalContainers() {
      this.loading = true;
      this.$http.get('/console/alarmStatistics/abnormalContainers').then((data) => {
        this.abnormalContainersList = JSON.parse(data);
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
