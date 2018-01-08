<template>
    <div class="dashboard-content">
        <el-card>
            <div slot="header">
                <span style="font-size: 23px;"><i class="fa fa-pie-chart"></i>dashboard</span>
                <el-select size="small" class="pull-right" v-model="clusterKey" @change="clusterChange">
                    <el-option label="全部集群" value=""></el-option>
                    <el-option v-for="item in clusterKeys" :label="item" :value="item" :key="item"></el-option>
                </el-select>
            </div>
            <div>
                <el-row :gutter="20">
                    <el-col :span="8">
                        <CardPanel type="primary" icon="fa fa-navicon" title="总域数" num="49" to-url="domain_statistic"></CardPanel>
                    </el-col>
                    <el-col :span="8">
                        <CardPanel type="green" icon="fa fa-server" title="Executor数: 物理机+容器" num="6+2" to-url="executor_statistic"></CardPanel>
                    </el-col>
                    <el-col :span="8">
                        <CardPanel type="yellow" icon="fa fa-cubes" title="总作业数" num="543" to-url="job_statistic"></CardPanel>
                    </el-col>
                </el-row>
            </div>
        </el-card>
    </div>
</template>
<script>
export default {
  props: [],
  data() {
    return {
      clusterKey: '',
      clusterKeys: [],
    };
  },
  methods: {
    clusterChange() {
      console.log('111');
    },
    getDashboardCount() {
      this.$http.get('/console/dashboard/count', { zkClusterKey: this.clusterKey }).then((data) => {
        console.log(JSON.stringify(data));
      })
      .catch(() => { this.$http.buildErrorHandler('获取dashboard统计请求失败！'); });
    },
  },
  created() {
    this.getDashboardCount();
  },
};
</script>
<style lang="sass" scoped>
.dashboard-content {
    margin: 20px;
}
</style>
