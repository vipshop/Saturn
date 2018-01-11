<template>
    <div class="alarm-center">
        <el-tabs v-model="activeName" @tab-click="onTabClick">
            <el-tab-pane name="job_abnormal_jobs"><span slot="label"><i class="fa fa-list-alt"></i>异常作业</span></el-tab-pane>
            <el-tab-pane name="job_timeout_jobs"><span slot="label"><i class="fa fa-clock-o"></i>超时作业</span></el-tab-pane>
            <el-tab-pane name="job_failover_jobs"><span slot="label"><i class="fa fa-exclamation-triangle"></i>无法高可用作业</span></el-tab-pane>
        </el-tabs>
        <router-view></router-view>
    </div>
</template>
<script>
export default {
  data() {
    return {
      domainName: this.$route.params.domain,
      jobName: this.$route.params.jobName,
      activeName: 'job_abnormal_jobs',
    };
  },
  methods: {
    onTabClick() {
      this.$router.push({
        name: this.activeName,
        params: {
          domain: this.$route.params.domain,
          jobName: this.$route.params.jobName,
        },
      });
    },
    getActiveName() {
      const str = this.$route.name;
      if (str) {
        this.activeName = str;
      }
    },
  },
  created() {
    this.activeName = this.$route.name;
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
</style>
