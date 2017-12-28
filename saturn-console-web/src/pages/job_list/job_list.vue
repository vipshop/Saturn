<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <Top-bar :domain="domainName" :domain-info="domainInfo"></Top-bar>
        <Aside :sidebar-menus="sidebarMenus">
            <router-view style="margin: 20px;"></router-view>
        </Aside>
    </div>
</template>

<script>
export default {
  data() {
    return {
      loading: false,
      domainName: this.$route.params.domain,
      domainInfo: {},
      sidebarMenus: [
        { index: 'job_overview', title: '作业总览', icon: 'fa fa-bar-chart-o fa-fw', name: 'job_overview', params: { domain: this.$route.params.domain } },
        { index: 'executor_overview', title: 'Executor总览', icon: 'fa fa-area-chart', name: 'executor_overview', params: { domain: this.$route.params.domain } },
      ],
    };
  },
  methods: {
    getDomainInfo() {
      this.loading = true;
      this.$http.get('/console/home/namespace', { namespace: this.domainName }).then((data) => {
        this.domainInfo = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取namespace信息请求失败！'); })
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
</style>
