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
                        <Chart-container title="失败率最高的Top10 Executor(当天)" type="bar">
                            <div slot="chart">
                                <Column id="top10FailExecutor" :option-info="top10FailExecutorOptionInfo"></Column>
                            </div>
                        </Chart-container>
                    </el-col>
                    <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                        <Chart-container title="负荷最重的Top10 Executor" type="bar">
                            <div slot="chart">
                                <Column id="top10LoadExecutor" :option-info="top10LoadExecutorOptionInfo"></Column>
                            </div>
                        </Chart-container>
                    </el-col>
                    <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                        <Chart-container title="Executor版本分布" type="pie">
                            <div slot="chart">
                                <Pie id="executorVersionNumber" :option-info="executorVersionNumberOptionInfo"></Pie>
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
      top10FailExecutorOptionInfo: {},
      top10LoadExecutorOptionInfo: {},
      executorVersionNumberOptionInfo: {},
    };
  },
  methods: {
    getTop10FailExecutor() {
      return this.$http.get('/console/dashboard/top10FailExecutor', { zkClusterKey: this.zkCluster }).then((data) => {
        const resultData = JSON.parse(data);
        const executors = [];
        const dataArr = [];
        resultData.forEach((ele) => {
          executors.push(ele.executorName);
          this.$set(ele, 'value', ele.failureRateOfTheDay);
          this.$set(ele, 'columnType', 'executor');
          dataArr.push(ele);
        });
        const tooltip = {
          position: 'inside',
          enterable: true,
          formatter(params) {
            return `<b>${params.name}</b><br/>
            错误率: ${params.value}<br/>
            执行总数: ${params.data.processCountOfTheDay}<br/>
            失败总数: ${params.data.failureCountOfTheDay}<br/>`;
          },
        };
        const optionInfo = {
          seriesData: dataArr,
          xCategories: executors,
          yTitle: '失败率(小数)',
          tooltip,
        };
        this.top10FailExecutorOptionInfo = optionInfo;
      })
      .catch(() => { this.$http.buildErrorHandler('获取失败率最高的Top10 Executor请求失败！'); });
    },
    getTop10LoadExecutor() {
      return this.$http.get('/console/dashboard/top10LoadExecutor', { zkClusterKey: this.zkCluster }).then((data) => {
        const resultData = JSON.parse(data);
        const executors = [];
        const dataArr = [];
        resultData.forEach((ele) => {
          executors.push(ele.executorName);
          this.$set(ele, 'value', ele.loadLevel);
          this.$set(ele, 'columnType', 'executor');
          dataArr.push(ele);
        });
        const tooltip = {
          position: 'inside',
          enterable: true,
          formatter(params) {
            return `<b>${params.name}</b><br/>
            所属域: ${params.data.domain}<br/>
            总负荷: ${params.data.loadLevel}<br/>
            作业与分片: ${params.data.jobAndShardings}<br/>`;
          },
        };
        const optionInfo = {
          seriesData: dataArr,
          xCategories: executors,
          yTitle: 'Executor总负荷',
          tooltip,
        };
        this.top10LoadExecutorOptionInfo = optionInfo;
      })
      .catch(() => { this.$http.buildErrorHandler('获取负荷最重的Top10 Executor请求失败！'); });
    },
    getExecutorVersionNumber() {
      return this.$http.get('/console/dashboard/executorVersionNumber', { zkClusterKey: this.zkCluster }).then((data) => {
        const resultData = [];
        Object.entries(data).forEach((ele) => {
          const item = { name: ele[0], value: ele[1] };
          resultData.push(item);
        });
        const seriesData = [{ name: '该版本Executor数量', type: 'pie', data: resultData }];
        this.$set(this.executorVersionNumberOptionInfo, 'seriesData', seriesData);
      })
      .catch(() => { this.$http.buildErrorHandler('获取Executor版本分布请求失败！'); });
    },
    init() {
      this.loading = true;
      Promise.all([this.getTop10FailExecutor(),
        this.getTop10LoadExecutor(), this.getExecutorVersionNumber()]).then(() => {
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
