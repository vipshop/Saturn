<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <Top-bar :domain="domainName" :domain-info="domainInfo"></Top-bar>
        <Aside :sidebar-menus="sidebarMenus" headerHeight="90">
            <router-view></router-view>
        </Aside>
    </div>
</template>

<script>
export default {
  data() {
    return {
      loading: false,
      domainInfo: {},
    };
  },
  methods: {
    getDomainInfo() {
      return this.$http.get(`/console/namespaces/${this.domainName}/`).then((data) => {
        this.domainInfo = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取namespace信息请求失败！'); });
    },
    init() {
      this.loading = true;
      Promise.all([this.getDomainInfo()]).then(() => {
        this.loading = false;
      });
    },
  },
  created() {
    this.init();
  },
  computed: {
    domainName() {
      return this.$route.params.domain;
    },
    sidebarMenus() {
      const menus = [
        { index: 'job_overview', title: '作业总览', icon: 'fa fa-bar-chart-o fa-fw', name: 'job_overview', params: { domain: this.$route.params.domain } },
        { index: 'executor_overview', title: 'Executor总览', icon: 'fa fa-area-chart', name: 'executor_overview', params: { domain: this.$route.params.domain } },
        { index: 'namespace_alarm_center', title: '告警中心', icon: 'fa fa-bell', name: 'namespace_abnormal_jobs', params: { domain: this.$route.params.domain } },
      ];
      return menus;
    },
  },
  watch: {
    $route: 'init',
  },
};
</script>
<style lang="sass" scoped>
</style>
