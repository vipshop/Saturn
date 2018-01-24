<template>
    <div class="page-content" v-loading="loading" element-loading-text="请稍等···">
        <div>
            <el-row :gutter="10">
                <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                    <Chart-container title="失败率最高的Top10作业">
                        <div slot="chart">
                            <Column id="top10FailJob" v-if="top10FailJobOption.optionInfo" :option-info="top10FailJobOption.optionInfo"></Column>
                        </div>
                    </Chart-container>
                </el-col>
                <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                    <Chart-container title="最活跃的Top10作业(当天执行次数最多的作业)">
                        <div slot="chart">
                            <Column id="top10ActiveJob" v-if="top10ActiveJobOption.optionInfo" :option-info="top10ActiveJobOption.optionInfo"></Column>
                        </div>
                    </Chart-container>
                </el-col>
                <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                    <Chart-container title="负荷最重的Top10作业">
                        <div slot="chart">
                            <Column id="top10LoadJob" v-if="top10LoadJobOption.optionInfo" :option-info="top10LoadJobOption.optionInfo"></Column>
                        </div>
                    </Chart-container>
                </el-col>
            </el-row>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      zkCluster: this.$route.query.zkCluster,
      top10FailJobOption: {},
      top10ActiveJobOption: {},
      top10LoadJobOption: {},
    };
  },
  methods: {
    getTop10FailJob() {
      return this.$http.get('/console/dashboard/top10FailJob', { zkClusterKey: this.zkCluster }).then((data) => {
        const resultData = JSON.parse(data);
        const jobs = [];
        const dataArr = [];
        resultData.forEach((ele) => {
          jobs.push(ele.jobName);
          this.$set(ele, 'y', ele.failureRateOfAllTime);
          this.$set(ele, 'columnType', 'job');
          dataArr.push(ele);
        });
        const tooltip = function setTooltip() {
          return `<b>${this.point.category}</b><br/>
          所属域: ${this.point.domainName}<br/>
          执行总数: ${this.point.processCountOfAllTime}<br/>
          失败总数: ${this.point.errorCountOfAllTime}<br/>
          <button class="chart-tooltip-btn" onclick="vm.clearZk('/console/dashboard/namespaces/${this.point.domainName}/jobs/${this.point.jobName}/jobAnalyse/clean')">清除zk</button>`;
        };
        const optionInfo = {
          seriesData: [{ data: dataArr }],
          xCategories: jobs,
          yTitle: '失败率(小数)',
          tooltip,
        };
        this.$set(this.top10FailJobOption, 'optionInfo', optionInfo);
      })
      .catch(() => { this.$http.buildErrorHandler('获取失败率最高的Top10作业请求失败！'); });
    },
    getTop10ActiveJob() {
      return this.$http.get('/console/dashboard/top10ActiveJob', { zkClusterKey: this.zkCluster }).then((data) => {
        const resultData = JSON.parse(data);
        const jobs = [];
        const dataArr = [];
        resultData.forEach((ele) => {
          jobs.push(ele.jobName);
          this.$set(ele, 'y', ele.processCountOfTheDay);
          this.$set(ele, 'columnType', 'job');
          dataArr.push(ele);
        });
        const tooltip = function setTooltip() {
          return `<b>${this.point.category}</b><br/>
          所属域: ${this.point.domainName}<br/>
          当天执行总数: ${this.point.processCountOfTheDay}<br/>
          当天失败数: ${this.point.failureCountOfTheDay}<br/>
          <button class="chart-tooltip-btn" onclick="vm.clearZk('/console/dashboard/namespaces/${this.point.domainName}/jobs/${this.point.jobName}/jobExecutorCount/clean')">清除zk</button>`;
        };
        const optionInfo = {
          seriesData: [{ data: dataArr }],
          xCategories: jobs,
          yTitle: '当天执行次数',
          tooltip,
        };
        this.$set(this.top10ActiveJobOption, 'optionInfo', optionInfo);
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
          this.$set(ele, 'y', ele.totalLoadLevel);
          this.$set(ele, 'columnType', 'job');
          dataArr.push(ele);
        });
        const tooltip = function setTooltip() {
          return `<b>${this.point.category}</b><br/>
          所属域: ${this.point.domainName}<br/>
          失败率: ${this.point.failureRateOfAllTime}<br/>
          总负荷: ${this.point.totalLoadLevel}<br/>`;
        };
        const optionInfo = {
          seriesData: [{ data: dataArr }],
          xCategories: jobs,
          yTitle: '作业总负荷',
          tooltip,
        };
        this.$set(this.top10LoadJobOption, 'optionInfo', optionInfo);
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
