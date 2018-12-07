<template>
    <div class="dashboard-content">
        <el-card>
            <div slot="header">
                <span style="font-size: 23px;"><i class="fa fa-pie-chart"></i>Dashboard</span>
                <el-select size="small" class="pull-right" v-model="clusterKey" @change="clusterChange">
                    <el-option label="全部集群" value=""></el-option>
                    <el-option v-for="item in onlineClusterkeys" :label="item.zkAlias" :value="item.zkClusterKey" :key="item.zkClusterKey"></el-option>
                </el-select>
            </div>
            <div>
                <el-row :gutter="20">
                    <el-col :span="8">
                        <CardPanel type="green" icon="fa fa-life-ring" title="总域数" :num="domainCount" to-url="domain_statistic" :url-query="clusterKey"></CardPanel>
                    </el-col>
                    <el-col :span="8">
                        <CardPanel type="yellow" icon="fa fa-server" title="Executor数: 物理机+容器" :num="executorCount" to-url="executor_statistic" :url-query="clusterKey"></CardPanel>
                    </el-col>
                    <el-col :span="8">
                        <CardPanel type="primary" icon="fa fa-list-alt" title="总作业数" :num="jobCount" to-url="job_statistic" :url-query="clusterKey"></CardPanel>
                    </el-col>
                </el-row>
            </div>
            <div>
                <el-row :gutter="10">
                    <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                        <Chart-container title="历史总域数" type="line">
                            <div slot="chart">
                                <MyLine id="domainCountHistory" :option-info="domainCountOptionInfo"></MyLine>
                            </div>
                        </Chart-container>
                    </el-col>
                    <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                        <Chart-container title="历史Executor数" type="line">
                            <div slot="chart">
                                <MyLine id="executorCountHistory" :option-info="executorCountOptionInfo"></MyLine>
                            </div>
                        </Chart-container>
                    </el-col>
                    <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                        <Chart-container title="历史作业数" type="line">
                            <div slot="chart">
                                <MyLine id="jobCountHistory" :option-info="jobCountOptionInfo"></MyLine>
                            </div>
                        </Chart-container>
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
      domainCount: '',
      executorInDockerCount: '',
      executorNotInDockerCount: '',
      jobCount: '',
      domainCountOptionInfo: {},
      executorCountOptionInfo: {},
      jobCountOptionInfo: {},
    };
  },
  methods: {
    clusterChange() {
      this.getDashboardCount();
    },
    getDashboardCount() {
      this.$http.get('/console/dashboard/count', { zkClusterKey: this.clusterKey }).then((data) => {
        this.domainCount = data.domainCount;
        this.executorInDockerCount = data.executorInDockerCount;
        this.executorNotInDockerCount = data.executorNotInDockerCount;
        this.jobCount = data.jobCount;
      })
      .catch(() => { this.$http.buildErrorHandler('获取dashboard统计请求失败！'); });
    },
    getZkClusters() {
      this.$http.get('/console/zkClusters').then((data) => {
        this.clusterKeys = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取所有zk集群请求失败！'); });
    },
    getDomainCountHistory() {
      return this.$http.get('/console/dashboard/domainCount', { zkClusterKey: this.clusterKey }).then((data) => {
        const optionInfo = {
          xAxis: data.xAxis,
          seriesData: [{ name: '总域数', data: data.yAxis }],
          yAxisName: '总域数',
        };
        this.domainCountOptionInfo = optionInfo;
      }).catch(() => { this.$http.buildErrorHandler('获取历史全域数据请求失败！'); });
    },
    getExecutorCountHistory() {
      return this.$http.get('/console/dashboard/executorCount', { zkClusterKey: this.clusterKey }).then((data) => {
        const optionInfo = {
          xAxis: data.date,
          seriesData: [{ name: '物理机', data: data.otherCount },
              { name: '容器', data: data.dockerCount },
              { name: '总数', data: data.totalCount }],
          yAxisName: 'executor数',
        };
        this.executorCountOptionInfo = optionInfo;
      }).catch(() => { this.$http.buildErrorHandler('获取Executor历史数据请求失败！'); });
    },
    getJobCountHistory() {
      return this.$http.get('/console/dashboard/jobCount', { zkClusterKey: this.clusterKey }).then((data) => {
        const optionInfo = {
          xAxis: data.xAxis,
          seriesData: [{ name: '作业数', data: data.yAxis }],
          yAxisName: '作业数',
        };
        this.jobCountOptionInfo = optionInfo;
      }).catch(() => { this.$http.buildErrorHandler('获取历史全域数据请求失败！'); });
    },
    updateHistories() {
      this.getDomainCountHistory();
      this.getExecutorCountHistory();
      this.getJobCountHistory();
    },
  },
  computed: {
    executorCount() {
      return `${this.executorNotInDockerCount}+${this.executorInDockerCount}`;
    },
    onlineClusterkeys() {
      const arr = [];
      this.clusterKeys.forEach((ele) => {
        if (!ele.offline) {
          arr.push(ele);
        }
      });
      return arr;
    },
  },
  created() {
    this.getZkClusters();
    this.getDashboardCount();
    this.getDomainCountHistory();
    this.getExecutorCountHistory();
    this.getJobCountHistory();
  },
  watch: {
    clusterKey: 'updateHistories',
  },
};
</script>
<style lang="sass" scoped>
.dashboard-content {
    margin: 20px;
}
</style>
