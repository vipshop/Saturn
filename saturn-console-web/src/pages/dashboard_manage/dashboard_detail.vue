<template>
    <div>
        <Aside :sidebar-menus="sidebarMenus" headerHeight="50">
            <router-view></router-view>
        </Aside>
    </div>
</template>

<script>
export default {
  data() {
    return {
      sidebarMenus: [
        { index: 'domain_statistic', title: '域统计', icon: 'fa fa-navicon', name: 'domain_statistic' },
        { index: 'executor_statistic', title: 'Executor统计', icon: 'fa fa-server', name: 'executor_statistic' },
        { index: 'job_statistic', title: '作业统计', icon: 'fa fa-cubes', name: 'job_statistic' },
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
