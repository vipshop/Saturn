<template>
    <div>
        <Aside :sidebar-menus="sidebarMenus" headerHeight="50">
            <div class="page-detail-header">
                <div class="pull-left page-detail-title">
                  <span class="page-detail-span">
                      <i class="fa fa-sitemap"></i>
                      当前ZK集群 : {{zkCluster || '全部ZK集群'}}
                  </span>
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
      zkCluster: this.$route.query.zkCluster,
      sidebarMenus: [
        { index: 'domain_statistic', title: '域统计', icon: 'fa fa-navicon', name: 'domain_statistic', query: { zkCluster: this.$route.query.zkCluster } },
        { index: 'executor_statistic', title: 'Executor统计', icon: 'fa fa-server', name: 'executor_statistic', query: { zkCluster: this.$route.query.zkCluster } },
        { index: 'job_statistic', title: '作业统计', icon: 'fa fa-cubes', name: 'job_statistic', query: { zkCluster: this.$route.query.zkCluster } },
      ],
    };
  },
  methods: {
  },
  created() {
  },
};
</script>
<style lang="sass" scoped>
</style>
