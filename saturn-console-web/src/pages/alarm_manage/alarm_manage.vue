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
        { index: 'alarm_abnormal_jobs', title: '异常作业', icon: 'fa fa-list-alt', name: 'alarm_abnormal_jobs', alarmCount: 0 },
        { index: 'alarm_timeout_jobs', title: '超时作业', icon: 'fa fa-clock-o', name: 'alarm_timeout_jobs', alarmCount: 0 },
        { index: 'unable_failover_jobs', title: '无法高可用', icon: 'fa fa-exclamation-triangle', name: 'unable_failover_jobs', alarmCount: 0 },
        // { index: 'alarm_abnormal_containers', title: '异常容器',
        // icon: 'fa fa-cube', name: 'alarm_abnormal_containers' },
      ],
    };
  },
  methods: {
    getCountOfAlarmJobs() {
      this.$http.get('/console/alarmStatistics/countOfAlarmJobs').then((data) => {
        data.forEach((ele) => {
          if (ele.alarmJobType === 'unnormal_job') {
            this.$set(this.sidebarMenus[0], 'alarmCount', ele.count);
          } else if (ele.alarmJobType === 'timeout_4_alarm_job') {
            this.$set(this.sidebarMenus[1], 'alarmCount', ele.count);
          } else if (ele.alarmJobType === 'unable_failover_job') {
            this.$set(this.sidebarMenus[2], 'alarmCount', ele.count);
          }
        });
      })
      .catch(() => { this.$http.buildErrorHandler('获取异常作业统计请求失败！'); });
    },
  },
  created() {
    this.getCountOfAlarmJobs();
  },
};
</script>
<style lang="sass" scoped>
</style>
