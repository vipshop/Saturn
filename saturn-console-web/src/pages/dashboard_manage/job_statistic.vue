<template>
    <div>
        <div class="page-detail-header">
            <div class="pull-left page-detail-title">
              <span class="page-detail-span">
                  <i class="fa fa-sitemap"></i>
                  当前ZK集群 : {{zkCluster || '全部ZK集群'}}
              </span>
              <el-button style="margin-left:10px;" type="text" @click="init"><i class="fa fa-refresh"></i></el-button>
            </div>
        </div>
        <div class="page-content" v-loading="loading" element-loading-text="请稍等···">
            <div>
                <el-row :gutter="10">
                    <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                        <Chart-container title="失败率最高的Top10作业" type="bar">
                            <div slot="chart">
                                <Column id="top10FailJob" :option-info="top10FailJobOptionInfo"></Column>
                            </div>
                        </Chart-container>
                    </el-col>
                    <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                        <Chart-container title="最活跃的Top10作业(当天执行次数最多的作业)" type="bar">
                            <div slot="chart">
                                <Column id="top10ActiveJob" :option-info="top10ActiveJobOptionInfo"></Column>
                            </div>
                        </Chart-container>
                    </el-col>
                    <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                        <Chart-container title="负荷最重的Top10作业" type="bar">
                            <div slot="chart">
                                <Column id="top10LoadJob" :option-info="top10LoadJobOptionInfo"></Column>
                            </div>
                        </Chart-container>
                    </el-col>
                </el-row>
            </div>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      zkCluster: this.$route.query.zkCluster,
      top10FailJobOptionInfo: {},
      top10ActiveJobOptionInfo: {},
      top10LoadJobOptionInfo: {},
    };
  },
  methods: {
    getTop10FailJob() {
      return this.$http.get('/console/dashboard/top10FailJob', { zkClusterKey: this.zkCluster }).then((data) => {
        const hasAuth = this.$common.hasPerm('dashboard:cleanOneJobAnalyse');
        const resultData = JSON.parse(data);
        const jobs = [];
        const dataArr = [];
        resultData.forEach((ele) => {
          jobs.push(ele.jobName);
          this.$set(ele, 'value', ele.failureRateOfAllTime);
          this.$set(ele, 'columnType', 'job');
          dataArr.push(ele);
        });
        const tooltip = {
          position: 'inside',
          enterable: true,
          formatter(params) {
            let result = '';
            const tooltipStr = `<b>${params.name}</b><br/>
            所属域: ${params.data.domainName}<br/>
            执行总数: ${params.data.processCountOfAllTime}<br/>
            失败总数: ${params.data.errorCountOfAllTime}<br/>`;
            if (hasAuth) {
              result = `${tooltipStr}<button class="chart-tooltip-btn" onclick="vm.clearZk('/console/dashboard/namespaces/${params.data.domainName}/jobs/${params.data.jobName}/jobAnalyse/clean')">清除zk</button>`;
            } else {
              result = tooltipStr;
            }
            return result;
          },
        };
        const optionInfo = {
          seriesData: dataArr,
          xCategories: jobs,
          yTitle: '失败率(小数)',
          tooltip,
        };
        this.top10FailJobOptionInfo = optionInfo;
      })
      .catch(() => { this.$http.buildErrorHandler('获取失败率最高的Top10作业请求失败！'); });
    },
    getTop10ActiveJob() {
      return this.$http.get('/console/dashboard/top10ActiveJob', { zkClusterKey: this.zkCluster }).then((data) => {
        const hasAuth = this.$common.hasPerm('dashboard:cleanOneJobExecutorCount');
        const resultData = JSON.parse(data);
        const jobs = [];
        const dataArr = [];
        resultData.forEach((ele) => {
          jobs.push(ele.jobName);
          this.$set(ele, 'value', ele.processCountOfTheDay);
          this.$set(ele, 'columnType', 'job');
          dataArr.push(ele);
        });
        const tooltip = {
          position: 'inside',
          enterable: true,
          formatter(params) {
            let result = '';
            const tooltipStr = `<b>${params.name}</b><br/>
            所属域: ${params.data.domainName}<br/>
            当天执行总数: ${params.data.processCountOfTheDay}<br/>
            当天失败数: ${params.data.failureCountOfTheDay}<br/>`;
            if (hasAuth) {
              result = `${tooltipStr}<button class="chart-tooltip-btn" onclick="vm.clearZk('/console/dashboard/namespaces/${params.data.domainName}/jobs/${params.data.jobName}/jobExecutorCount/clean')">清除zk</button>`;
            } else {
              result = tooltipStr;
            }
            return result;
          },
        };
        const optionInfo = {
          seriesData: dataArr,
          xCategories: jobs,
          yTitle: '当天执行次数',
          tooltip,
        };
        this.top10ActiveJobOptionInfo = optionInfo;
      })
      .catch(() => { this.$http.buildErrorHandler('获取最活跃的Top10作业请求失败！'); });
    },
    getTop10LoadJob() {
      return this.$http.get('/console/dashboard/top10LoadJob', { zkClusterKey: this.zkCluster }).then((data) => {
        const resultData = JSON.parse(data);
        const jobs = [];
        const dataArr = [];
        resultData.forEach((ele) => {
          jobs.push(ele.jobName);
          this.$set(ele, 'value', ele.totalLoadLevel);
          this.$set(ele, 'columnType', 'job');
          dataArr.push(ele);
        });
        const tooltip = {
          position: 'inside',
          enterable: true,
          formatter(params) {
            return `<b>${params.name}</b><br/>
            所属域: ${params.data.domainName}<br/>
            失败率: ${params.data.failureRateOfAllTime}<br/>
            总负荷: ${params.data.totalLoadLevel}<br/>`;
          },
        };
        const optionInfo = {
          seriesData: dataArr,
          xCategories: jobs,
          yTitle: '作业总负荷',
          tooltip,
        };
        this.top10LoadJobOptionInfo = optionInfo;
      })
      .catch(() => { this.$http.buildErrorHandler('获取负荷最重的Top10作业请求失败！'); });
    },
    init() {
      this.loading = true;
      Promise.all([this.getTop10FailJob(),
        this.getTop10ActiveJob(), this.getTop10LoadJob()]).then(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.init();
  },
};
</script>
<style lang="sass" scoped>
</style>
