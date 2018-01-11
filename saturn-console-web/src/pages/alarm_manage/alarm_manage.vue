<template>
    <div>
        <Aside :sidebar-menus="sidebarMenus" headerHeight="50">
            <router-view style="margin: 20px;"></router-view>
        </Aside>
    </div>
</template>

<script>
export default {
  data() {
    return {
      sidebarMenus: [
        { index: 'alarm_abnormal_jobs', title: '异常作业', icon: 'fa fa-list-alt', name: 'alarm_abnormal_jobs' },
        { index: 'alarm_timeout_jobs', title: '超时作业', icon: 'fa fa-clock-o', name: 'alarm_timeout_jobs' },
        { index: 'unable_failover_jobs', title: '无法高可用', icon: 'fa fa-exclamation-triangle', name: 'unable_failover_jobs' },
        { index: 'alarm_abnormal_containers', title: '异常容器', icon: 'fa fa-cube', name: 'alarm_abnormal_containers' },
      ],
    };
  },
  methods: {
  },
};
</script>
<style lang="sass" scoped>
</style>
