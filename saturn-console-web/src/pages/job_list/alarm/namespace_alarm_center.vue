<template>
    <div class="alarm-center">
        <el-tabs v-model="activeName" @tab-click="onTabClick">
            <el-tab-pane name="namespace_abnormal_jobs">
              <span slot="label"><i class="fa fa-list-alt"></i>异常作业
                <el-tag type="danger" class="alarm-tag" v-if="countOfAlarmJobs.unnormal_job">{{countOfAlarmJobs.unnormal_job}}</el-tag>
              </span>
            </el-tab-pane>
            <el-tab-pane name="namespace_timeout_jobs">
              <span slot="label"><i class="fa fa-clock-o"></i>超时作业
                <el-tag type="danger" class="alarm-tag" v-if="countOfAlarmJobs.timeout_4_alarm_job">{{countOfAlarmJobs.timeout_4_alarm_job}}</el-tag>
              </span>
            </el-tab-pane>
            <el-tab-pane name="namespace_failover_jobs">
              <span slot="label"><i class="fa fa-exclamation-triangle"></i>无法高可用作业
                <el-tag type="warning" class="alarm-tag" v-if="countOfAlarmJobs.unable_failover_job">{{countOfAlarmJobs.unable_failover_job}}</el-tag>
              </span>
            </el-tab-pane>
            <!-- <el-tab-pane name="namespace_abnormal_containers"><span slot="label"><i class="fa fa-cube"></i>异常容器</span></el-tab-pane> -->
        </el-tabs>
        <router-view></router-view>
    </div>
</template>
<script>
export default {
  data() {
    return {
      activeName: 'namespace_abnormal_jobs',
      countOfAlarmJobs: {
        unnormal_job: 0,
        timeout_4_alarm_job: 0,
        unable_failover_job: 0,
      },
    };
  },
  methods: {
    onTabClick() {
      this.$router.push({
        name: this.activeName,
        params: {
          domain: this.$route.params.domain,
        },
      });
    },
    getActiveName() {
      const str = this.$route.name;
      if (str) {
        this.activeName = str;
      }
    },
    getCountOfAlarmJobs() {
      this.$http.get(`/console/namespaces/${this.domainName}/alarmStatistics/countOfAlarmJobs`).then((data) => {
        data.forEach((ele) => {
          if (ele.alarmJobType === 'unnormal_job') {
            this.$set(this.countOfAlarmJobs, 'unnormal_job', ele.count);
          } else if (ele.alarmJobType === 'timeout_4_alarm_job') {
            this.$set(this.countOfAlarmJobs, 'timeout_4_alarm_job', ele.count);
          } else if (ele.alarmJobType === 'unable_failover_job') {
            this.$set(this.countOfAlarmJobs, 'unable_failover_job', ele.count);
          }
        });
      })
      .catch(() => { this.$http.buildErrorHandler('获取异常作业统计请求失败！'); });
    },
  },
  created() {
    this.activeName = this.$route.name;
    this.getCountOfAlarmJobs();
  },
  computed: {
    domainName() {
      return this.$route.params.domain;
    },
  },
  watch: {
    $route: 'getActiveName',
  },
};
</script>
<style lang="sass" scoped>
.alarm-center {
    margin: 10px 20px;
}
.alarm-tag {
    border-radius: 6px;
    margin-left: 3px;
    min-width: 26px;
    padding: 0 5px;
    text-align: center;
    height: 20px;
    line-height: 20px;
}
</style>
