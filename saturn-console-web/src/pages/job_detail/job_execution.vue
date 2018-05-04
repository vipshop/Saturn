<template>
    <div class="page-content">
        <div class="page-table">
            <div class="page-table-header">
                <div class="page-table-header-title"><i class="fa fa-list"></i>运行状态
                    <el-button type="text" @click="getJobExecutors()"><i class="fa fa-refresh"></i></el-button>
                </div>
                <div class="page-table-header-separator"></div>
                <div>
                    <span class="refresh-text">自动刷新：</span>
                    <el-select v-model="autoRefreshTime" placeholder="请选择" size="mini" style="width: 120px;" @change="refreshExecutions">
                        <el-option v-for="item in refreshTimes" :value="item.value" :label="item.label" :key="item.value"></el-option>
                    </el-select>
                </div>
            </div>
            <el-table stripe border :data="executions" style="width: 100%">
                <el-table-column prop="item" label="分片项" width="80px"></el-table-column>
                <el-table-column label="状态" min-width="60px">
                    <template slot-scope="scope"> 
                        <el-tag :type="statusTag[scope.row.status]">{{translateStatus[scope.row.status]}}</el-tag>
                        <el-tag type="danger" v-if="scope.row.failover">failover</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="jobMsg" label="分片返回信息">
                    <template slot-scope="scope"> 
                        {{scope.row.jobMsg}}
                    </template>
                </el-table-column>
                <el-table-column prop="executorName" label="Executor"></el-table-column>
                <el-table-column label="最近执行时间" width="180px">
                    <template slot-scope="scope"> 
                        <div>起:{{scope.row.lastBeginTime || ' ——'}}</div>
                        <div>止:{{scope.row.lastCompleteTime || ' ——'}}</div>
                    </template>
                </el-table-column>
                <el-table-column prop="lastTimeConsumedInSec" label="执行时长(秒)" width="110px">
                    <template slot-scope="scope"> 
                        <div v-if="scope.row.lastTimeConsumedInSec === 0">——</div>
                        <div v-else>{{scope.row.lastTimeConsumedInSec}}</div>
                    </template>
                </el-table-column>
                <el-table-column prop="nextFireTime" label="下次开始时间" width="180px"></el-table-column>
                <el-table-column label="日志" width="50px" align="center">
                    <template slot-scope="scope"> 
                        <el-tooltip content="查看日志" placement="top">
                            <el-button type="text" @click="getLog(scope.row)"><i class="fa fa-file-text-o"></i></el-button>
                        </el-tooltip>
                    </template>
                </el-table-column>
            </el-table>
        </div>
        <div v-if="isViewLogVisible">
            <ViewContentDialog title="查看日志" :content="logContent" @close-dialog="closeViewLogDialog"></ViewContentDialog>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      isViewLogVisible: false,
      autoRefreshTime: '',
      refreshTimes: [{
        label: '关闭',
        value: '',
      }, {
        label: '30秒',
        value: 30000,
      }, {
        label: '1分钟',
        value: 60000,
      }, {
        label: '3分钟',
        value: 180000,
      }, {
        label: '5分钟',
        value: 300000,
      }],
      statusTag: {
        RUNNING: 'success',
        COMPLETED: 'primary',
        FAILED: 'danger',
        TIMEOUT: 'warning',
        PENDING: 'warning',
      },
      translateStatus: {
        RUNNING: '运行中',
        COMPLETED: '完成',
        FAILED: '失败',
        TIMEOUT: '超时',
        PENDING: '未知',
      },
      executions: [],
      interval: 0,
      logContent: '',
    };
  },
  methods: {
    getLog(row) {
      const params = {
        jobItem: row.item,
      };
      this.$http.get(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/execution/log`, params).then((data) => {
        this.logContent = data;
        this.isViewLogVisible = true;
      })
      .catch(() => { this.$http.buildErrorHandler('获取日志请求失败！'); });
    },
    closeViewLogDialog() {
      this.isViewLogVisible = false;
    },
    refreshExecutions() {
      if (this.autoRefreshTime) {
        this.interval = setInterval(() => {
          this.init();
        }, this.autoRefreshTime);
      } else {
        clearInterval(this.interval);
      }
    },
    getJobExecutors() {
      return this.$http.get(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/execution/status`).then((data) => {
        this.executions = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取作业运行状态请求失败！'); });
    },
    getJobInfo() {
      const params = {
        domainName: this.domainName,
        jobName: this.jobName,
      };
      return this.$store.dispatch('setJobInfo', params).then(() => {})
      .catch(() => this.$http.buildErrorHandler('获取作业信息请求失败！'));
    },
    init() {
      Promise.all([this.getJobExecutors(), this.getJobInfo()]).then(() => {});
    },
  },
  watch: {
    $route: 'init',
  },
  computed: {
    domainName() {
      return this.$route.params.domain;
    },
    jobName() {
      return this.$route.params.jobName;
    },
  },
  created() {
    this.init();
  },
  destroyed() {
    clearInterval(this.interval);
  },
};
</script>
<style lang="sass" scoped>
.refresh-text {
    font-weight: bold;
    font-size: 13px;
}
</style>
