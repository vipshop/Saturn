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
                        <Chart-container title="全域当天执行数据">
                            <div slot="chart">
                                <Pie id="domainProcessCount" :data-option="domainProcessCountOption"></Pie>
                            </div>
                        </Chart-container>
                    </el-col>
                    <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                        <Chart-container title="失败率最高的Top10域">
                            <div slot="chart">
                                <Column id="top10FailDomain" :option-info="top10FailDomainOption.optionInfo"></Column>
                            </div>
                        </Chart-container>
                    </el-col>
                    <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                        <Chart-container title="稳定性最差的的Top10域">
                            <div slot="chart">
                                <Column id="top10UnstableDomain" :option-info="top10UnstableDomainOption.optionInfo"></Column>
                            </div>
                        </Chart-container>
                    </el-col>
                    <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                        <Chart-container title="域版本分布">
                            <div slot="chart">
                                <Pie id="domainExecutorVersion" :data-option="domainExecutorVersionOption"></Pie>
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
      domainProcessCountOption: {},
      domainExecutorVersionOption: {},
      top10FailDomainOption: {
        optionInfo: {},
      },
      top10UnstableDomainOption: {
        optionInfo: {},
      },
    };
  },
  methods: {
    getDomainProcessCount() {
      return this.$http.get('/console/dashboard/domainProcessCount', { zkClusterKey: this.zkCluster }).then((data) => {
        const resultData = JSON.parse(data);
        const error = resultData.error;
        const count = resultData.count;
        const dataArr = [['成功次数', Number.parseInt(count - error, 10)], ['失败次数', error]];
        const seriesData = [{ name: '全域当天执行数据', data: dataArr }];
        this.$set(this.domainProcessCountOption, 'seriesData', seriesData);
      })
      .catch(() => { this.$http.buildErrorHandler('获取全域当天执行数据请求失败！'); });
    },
    getTop10FailDomain() {
      return this.$http.get('/console/dashboard/top10FailDomain', { zkClusterKey: this.zkCluster }).then((data) => {
        const hasAuth = this.$common.hasPerm('dashboard:cleanAllJobAnalyse');
        const resultData = JSON.parse(data);
        const domains = [];
        const dataArr = [];
        resultData.forEach((ele) => {
          domains.push(ele.domainName);
          this.$set(ele, 'y', ele.failureRateOfAllTime);
          this.$set(ele, 'columnType', 'domain');
          dataArr.push(ele);
        });
        const tooltip = function setTooltip() {
          let result = '';
          const tooltipStr = `<b>${this.point.category}</b><br/>
          错误率: ${this.point.y}<br/>
          执行总数: ${this.point.processCountOfAllTime}<br/>
          失败总数: ${this.point.errorCountOfAllTime}<br/>`;
          if (hasAuth) {
            result = `${tooltipStr}<button class="chart-tooltip-btn" onclick="vm.clearZk('/console/dashboard/namespaces/${this.point.domainName}/jobAnalyse/clean')">清除zk</button>`;
          } else {
            result = tooltipStr;
          }
          return result;
        };
        const optionInfo = {
          seriesData: [{ data: dataArr }],
          xCategories: domains,
          yTitle: '失败率(小数)',
          tooltip,
        };
        this.$set(this.top10FailDomainOption, 'optionInfo', optionInfo);
      })
      .catch(() => { this.$http.buildErrorHandler('获取失败率最高的Top10域请求失败！'); });
    },
    getTop10UnstableDomain() {
      return this.$http.get('/console/dashboard/top10UnstableDomain', { zkClusterKey: this.zkCluster }).then((data) => {
        const hasAuth = this.$common.hasPerm('dashboard:cleanShardingCount');
        const resultData = JSON.parse(data);
        const domains = [];
        const dataArr = [];
        resultData.forEach((ele) => {
          domains.push(ele.domainName);
          this.$set(ele, 'y', ele.shardingCount);
          this.$set(ele, 'columnType', 'domain');
          dataArr.push(ele);
        });
        const tooltip = function setTooltip() {
          let result = '';
          const tooltipStr = `<b>${this.point.category}</b><br/>
          分片次数: ${this.point.y}<br/>`;
          if (hasAuth) {
            result = `${tooltipStr}<button class="chart-tooltip-btn" onclick="vm.clearZk('/console/dashboard/namespaces/${this.point.domainName}/shardingCount/clean')">清除zk</button>`;
          } else {
            result = tooltipStr;
          }
          return result;
        };
        const optionInfo = {
          seriesData: [{ data: dataArr }],
          xCategories: domains,
          yTitle: '分片次数',
          tooltip,
        };
        this.$set(this.top10UnstableDomainOption, 'optionInfo', optionInfo);
      })
      .catch(() => { this.$http.buildErrorHandler('获取稳定性最差的Top10域请求失败！'); });
    },
    getDomainExecutorVersionNumber() {
      return this.$http.get('/console/dashboard/domainExecutorVersionNumber', { zkClusterKey: this.zkCluster }).then((data) => {
        const arr = [];
        Object.entries(data).forEach((ele) => {
          if (ele[0] === '-1') {
            const a1 = ['未知版本', ele[1]];
            arr.push(a1);
          } else {
            arr.push(ele);
          }
        });
        const seriesData = [{ name: '该版本域数量', data: arr }];
        this.$set(this.domainExecutorVersionOption, 'seriesData', seriesData);
      })
      .catch(() => { this.$http.buildErrorHandler('获取域版本分布请求失败！'); });
    },
    init() {
      this.loading = true;
      Promise.all(
        [this.getDomainProcessCount(), this.getTop10FailDomain(),
          this.getTop10UnstableDomain(), this.getDomainExecutorVersionNumber()]).then(() => {
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
