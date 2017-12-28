<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <Top-bar :domain="domainName" :domain-info="domainInfo"></Top-bar>
        <Aside :sidebar-menus="sidebarMenus">
            <div class="job-detail-header">
                <div class="pull-left job-detail-title">作业 :  {{jobName}}</div>
                <div class="pull-right">
                    <el-button size="small" @click=""><i class="fa fa-play-circle text-btn"></i>启用</el-button>
                    <el-button size="small" @click=""><i class="fa fa-play-circle-o text-btn"></i>立即执行</el-button>
                    <el-button size="small" @click=""><i class="fa fa-stop-circle-o text-btn"></i>立即终止</el-button>
                    <el-button size="small" @click=""><i class="fa fa-trash text-btn"></i>删除</el-button>
                </div>
            </div>
            <router-view></router-view>
        </Aside>
    </div>
</template>

<script>
export default {
  data() {
    return {
      loading: false,
      domainName: this.$route.params.domain,
      jobName: this.$route.params.jobName,
      sidebarMenus: [
        { index: 'job_setting', title: '作业设置', icon: 'fa fa-gear', name: 'job_setting', params: { domain: this.$route.params.domain, jobName: this.$route.params.jobName } },
        { index: 'job_executor', title: '分片情况', icon: 'fa fa-server', name: 'job_executor', params: { domain: this.$route.params.domain, jobName: this.$route.params.jobName } },
        { index: 'running_state', title: '运行状态', icon: 'fa fa-dot-circle-o', name: 'running_state', params: { domain: this.$route.params.domain, jobName: this.$route.params.jobName } },
        { index: 'job_statistics', title: '作业统计', icon: 'fa fa-bar-chart', name: 'job_statistics', params: { domain: this.$route.params.domain, jobName: this.$route.params.jobName } },
      ],
      domainInfo: {},
    };
  },
  methods: {
    getDomainInfo() {
      this.loading = true;
      this.$http.get('/console/home/namespace', { namespace: this.domainName }).then((data) => {
        this.domainInfo = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取namespaces信息请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
  },
  created() {
    this.getDomainInfo();
  },
};
</script>
<style lang="sass" scoped>
.job-detail-header {
  padding: 10px;
  border-bottom: 1px solid #d0d0d0;
  height: 30px;
  .job-detail-title {
    height: 30px;
    line-height: 30px;
    font-weight: bold;
    font-size: 16px;
    color: #777;
  }
  >* {
    display: inline-block;
  }
}
</style>
