<template>
    <div>
        <VAside :sidebar-menus="sidebarMenus" headerHeight="50">
            <router-view></router-view>
        </VAside>
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
