<template>
    <div>
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
      this.$http.getData(`/console/home/namespaces/${this.domainName}`).then((data) => {
        if (data) {
          this.domainInfo = data;
        }
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
