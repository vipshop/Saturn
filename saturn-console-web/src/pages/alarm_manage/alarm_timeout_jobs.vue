<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <FilterPageList :data="timeoutJobsList" :total="total" :order-by="orderBy" :filters="filters">
                <template slot-scope="scope">
                    <el-form :inline="true" class="table-filter">
                        <el-form-item label="">
                            <el-input placeholder="搜索" v-model="filters.jobName"></el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" icon="el-icon-search" @click="scope.search">查询</el-button>
                        </el-form-item>
                    </el-form>
                    <div class="page-table" v-loading="loading" element-loading-text="请稍等···">
                        <div class="page-table-header">
                            <div class="page-table-header-title"><i class="fa fa-list"></i>运行超时（告警）的作业分片
                                <el-button type="text" @click="getTimeoutJobs"><i class="fa fa-refresh"></i></el-button>
                            </div>
                        </div>
                        <el-table stripe border @sort-change="scope.onSortChange" :data="scope.pageData" style="width: 100%">
                            <el-table-column prop="jobName" label="作业名" sortable>
                                <template slot-scope="scope">
                                    <router-link tag="a" :to="{ name: 'job_setting', params: { domain: scope.row.domainName, jobName: scope.row.jobName } }">
                                        <el-button type="text">
                                            {{scope.row.jobName}}
                                        </el-button>
                                    </router-link>
                                </template>
                            </el-table-column>
                            <el-table-column prop="domainName" label="所属域" sortable>
                                <template slot-scope="scope">
                                    <router-link tag="a" :to="{ name: 'job_overview', params: { domain: scope.row.domainName } }">
                                        <el-button type="text">
                                            {{scope.row.domainName}}
                                        </el-button>
                                    </router-link>
                                </template>
                            </el-table-column>
                            <el-table-column prop="degree" label="域等级" sortable>
                                <template slot-scope="scope">
                                    {{$map.degreeMap[scope.row.degree]}}
                                </template>
                            </el-table-column>
                            <el-table-column prop="jobDegree" label="作业等级" sortable>
                                <template slot-scope="scope">
                                    {{$map.degreeMap[scope.row.jobDegree]}}
                                </template>
                            </el-table-column>
                            <el-table-column prop="timeout4AlarmSeconds" label="超时秒数"></el-table-column>
                            <el-table-column prop="timeoutItems" label="超时分片"></el-table-column>
                            <el-table-column label="操作" width="100px" align="center">
                                <template slot-scope="scope">
                                    <el-button size="small" type="primary" @click="handleRead(scope.row)" :disabled="scope.row.read">不再告警</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                </template>
            </FilterPageList>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      timeoutJobsList: [],
      filters: {
        jobName: '',
      },
      orderBy: 'jobName',
      total: 0,
    };
  },
  methods: {
    handleRead(row) {
      this.$http.post('/console/zkClusters/alarmStatistics/setTimeout4AlarmJobMonitorStatusToRead', { uuid: row.uuid }).then(() => {
        this.getTimeoutJobs();
        this.$message.successNotify('操作成功');
      })
      .catch(() => { this.$http.buildErrorHandler('不再告警操作请求失败！'); });
    },
    getTimeoutJobs() {
      this.loading = true;
      this.$http.get('/console/zkClusters/alarmStatistics/timeout4AlarmJobs').then((data) => {
        this.timeoutJobsList = JSON.parse(data);
        this.total = this.timeoutJobsList.length;
      })
      .catch(() => { this.$http.buildErrorHandler('获取异常作业请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
  },
  created() {
    this.getTimeoutJobs();
  },
};
</script>
