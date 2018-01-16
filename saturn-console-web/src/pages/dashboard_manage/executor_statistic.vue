<template>
    <div class="page-content">
        <div>
            <el-row :gutter="10">
                <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                    <Chart-container title="失败率最高的Top10 Executor(当天)">
                        <div slot="chart">
                            <Column id="top10FailExecutor" v-if="top10FailExecutorOption.optionInfo" :option-info="top10FailExecutorOption.optionInfo"></Column>
                        </div>
                    </Chart-container>
                </el-col>
                <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                    <Chart-container title="负荷最重的Top10 Executor">
                        <div slot="chart">
                            <Column id="top10LoadExecutor" v-if="top10LoadExecutorOption.optionInfo" :option-info="top10LoadExecutorOption.optionInfo"></Column>
                        </div>
                    </Chart-container>
                </el-col>
                <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                    <Chart-container title="Executor版本分布">
                        <div slot="chart">
                            <Pie id="executorVersionNumber" :data-option="executorVersionNumberOption"></Pie>
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
      zkCluster: this.$route.query.zkCluster,
      top10FailExecutorOption: {},
      top10LoadExecutorOption: {},
      executorVersionNumberOption: {},
    };
  },
  methods: {
    getTop10FailExecutor() {
      this.$http.get('/console/dashboard/top10FailExecutor', { zkClusterKey: this.zkCluster }).then((data) => {
        const resultData = JSON.parse(data);
        const executors = [];
        const dataArr = [];
        resultData.forEach((ele) => {
          executors.push(ele.executorName);
          this.$set(ele, 'y', ele.failureRateOfTheDay);
          this.$set(ele, 'columnType', 'executor');
          dataArr.push(ele);
        });
        const tooltip = function setTooltip() {
          return `<b>${this.point.category}</b><br/>
          错误率: ${this.point.y}<br/>
          执行总数: ${this.point.processCountOfTheDay}<br/>
          失败总数: ${this.point.failureCountOfTheDay}<br/>`;
        };
        const optionInfo = {
          seriesData: [{ data: dataArr }],
          xCategories: executors,
          yTitle: '失败率(小数)',
          tooltip,
        };
        this.$set(this.top10FailExecutorOption, 'optionInfo', optionInfo);
      })
      .catch(() => { this.$http.buildErrorHandler('获取失败率最高的Top10 Executor请求失败！'); });
    },
    getTop10LoadExecutor() {
      this.$http.get('/console/dashboard/top10LoadExecutor', { zkClusterKey: this.zkCluster }).then((data) => {
        const resultData = JSON.parse(data);
        const executors = [];
        const dataArr = [];
        resultData.forEach((ele) => {
          executors.push(ele.executorName);
          this.$set(ele, 'y', ele.loadLevel);
          this.$set(ele, 'columnType', 'executor');
          dataArr.push(ele);
        });
        const tooltip = function setTooltip() {
          return `<b>${this.point.category}</b><br/>
          所属域: ${this.point.domain}<br/>
          总负荷: ${this.point.loadLevel}<br/>
          作业与分片: ${this.point.jobAndShardings}<br/>`;
        };
        const optionInfo = {
          seriesData: [{ data: dataArr }],
          xCategories: executors,
          yTitle: 'Executor总负荷',
          tooltip,
        };
        this.$set(this.top10LoadExecutorOption, 'optionInfo', optionInfo);
      })
      .catch(() => { this.$http.buildErrorHandler('获取负荷最重的Top10 Executor请求失败！'); });
    },
    getExecutorVersionNumber() {
      this.$http.get('/console/dashboard/executorVersionNumber', { zkClusterKey: this.zkCluster }).then((data) => {
        const seriesData = [{ name: '该版本Executor数量', data: Object.entries(data) }];
        this.$set(this.executorVersionNumberOption, 'seriesData', seriesData);
      })
      .catch(() => { this.$http.buildErrorHandler('获取Executor版本分布请求失败！'); });
    },
  },
  created() {
    this.getTop10FailExecutor();
    this.getTop10LoadExecutor();
    this.getExecutorVersionNumber();
  },
};
</script>
<style lang="sass" scoped>
</style>
