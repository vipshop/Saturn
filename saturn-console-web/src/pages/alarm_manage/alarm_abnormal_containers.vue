<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="page-container">
            <FilterPageList :data="abnormalContainersList" :total="total" :order-by="orderBy" :filters="filters">
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
                            <div class="page-table-header-title"><i class="fa fa-list"></i>异常容器
                                <el-button type="text" @click="getAbnormalContainers"><i class="fa fa-refresh"></i></el-button>
                            </div>
                        </div>
                        <el-table stripe border @sort-change="scope.onSortChange" :data="scope.pageData" style="width: 100%">
                            <el-table-column prop="containerName" label="容器名"></el-table-column>
                            <el-table-column prop="domainName" label="所属域"></el-table-column>
                            <el-table-column prop="degree" label="域等级"></el-table-column>
                            <el-table-column prop="jobDegree" label="作业等级"></el-table-column>
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
      abnormalContainersList: [],
      filters: {
        containerName: '',
      },
      orderBy: 'containerName',
      total: 0,
    };
  },
  methods: {
    getAbnormalContainers() {
      this.loading = true;
      this.$http.get('/console/zkClusters/alarmStatistics/abnormalContainers').then((data) => {
        this.abnormalContainersList = JSON.parse(data);
        this.total = this.abnormalContainersList.length;
      })
      .catch(() => { this.$http.buildErrorHandler('获取异常作业请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
  },
  created() {
    this.getAbnormalContainers();
  },
};
</script>
