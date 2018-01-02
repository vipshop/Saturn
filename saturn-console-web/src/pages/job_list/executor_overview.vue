<template>
    <div>
        <div>
            <el-row :gutter="20">
                <el-col :span="8">
                    <Panel type="success">
                        <div slot="title">在线数 / 总数</div>
                        <div slot="content">1 / 2</div>
                    </Panel>
                </el-col>
            </el-row>
        </div>
        <div class="page-container">
            <FilterPageList :data="executorList" :total="total" :order-by="orderBy" :filters="filters">
                <template slot-scope="scope">
                    <el-form :inline="true" class="table-filter">
                        <el-form-item label="">
                            <el-input placeholder="搜索" v-model="filters.executorName"></el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" icon="el-icon-search" @click="scope.search">查询</el-button>
                            <el-button type="primary" @click="handleReArrange()"><i class="fa fa-repeat"></i>一键重排</el-button>
                        </el-form-item>
                    </el-form>
                    <div class="page-table" v-loading="loading" element-loading-text="请稍等···">
                        <div class="page-table-header">
                            <div class="page-table-header-title"><i class="fa fa-list"></i>Executor列表
                                <el-button type="text" @click="getExecutorList"><i class="fa fa-refresh"></i></el-button>
                            </div>
                            <div class="page-table-header-separator"></div>
                            <div>
                                <el-button @click="batchDelete()"><i class="fa fa-trash text-btn"></i>删除</el-button>
                            </div>
                        </div>
                        <el-table stripe border ref="multipleTable" @selection-change="handleSelectionChange" @sort-change="scope.onSortChange" :data="scope.pageData" style="width: 100%">
                            <el-table-column type="selection" width="55"></el-table-column>
                            <el-table-column prop="executorName" label="Executor"></el-table-column>
                            <el-table-column label="状态">
                                <template slot-scope="scope"> 
                                    <el-tag :type="scope.row.status === 'ONLINE' ? 'success' : 'danger'" close-transition>{{scope.row.status}}</el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column prop="serverIp" label="IP"></el-table-column>
                            <el-table-column prop="groupName" label="分组"></el-table-column>
                            <el-table-column label="获取作业分片分配" header-align="left" align="center">
                                <template slot-scope="scope">
                                    <el-tooltip content="查看" placement="top">
                                        <el-button type="text" @click="getExecutorAllocation(scope.row)"><i class="fa fa-search-plus"></i></el-button>
                                    </el-tooltip>
                                </template>
                            </el-table-column>
                            <el-table-column prop="version" label="版本"></el-table-column>
                            <el-table-column prop="lastBeginTime" label="启动时间" min-width="100px"></el-table-column>
                            <el-table-column label="操作" width="110px">
                                <template slot-scope="scope">
                                    <el-tooltip content="摘取流量" placement="top" v-if="!scope.row.noTraffic">
                                        <el-button type="text" @click="handleTraffic(scope.row, 'extract')"><i class="fa fa-hand-lizard-o"></i></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="恢复流量" placement="top" v-if="scope.row.noTraffic">
                                        <el-button type="text" @click="handleTraffic(scope.row, 'recover')"><i class="fa fa-hand-paper-o"></i></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="重启" placement="top">
                                        <el-button type="text" @click="handleReset(scope.row)"><i class="fa fa-power-off"></i></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="删除" placement="top" v-if="scope.row.status === 'OFFLINE'">
                                        <el-button type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"></el-button>
                                    </el-tooltip>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                </template>
            </FilterPageList>
            <div v-if="isExecutorAllocationVisible">
                <executor-allocation-dialog :executor-allocation-info="executorAllocationInfo" @close-dialog="closeExecutorAllocationDialog"></executor-allocation-dialog>
            </div>
        </div>
    </div>
</template>

<script>
import executorAllocationDialog from './executor_allocation_dialog';

export default {
  data() {
    return {
      domainName: this.$route.params.domain,
      loading: false,
      executorList: [],
      filters: {
        executorName: '',
      },
      orderBy: 'executorName',
      total: 0,
      multipleSelection: [],
      isExecutorAllocationVisible: false,
      executorAllocationInfo: {},
    };
  },
  methods: {
    getExecutorAllocation(row) {
      const params = {
        namespace: this.domainName,
        executorName: row.executorName,
      };
      this.$http.get('/console/executor-overview/executor-allocation', params).then((data) => {
        this.isExecutorAllocationVisible = true;
        this.executorAllocationInfo = JSON.parse(JSON.stringify(data));
      })
      .catch(() => { this.$http.buildErrorHandler('获取作业分片分配失败！'); });
    },
    closeExecutorAllocationDialog() {
      this.isExecutorAllocationVisible = false;
    },
    handleReArrange() {
      this.$message.confirmMessage('确认一键重排吗?', () => {
        this.$http.post('/console/executor-overview/shard-all', { namespace: this.domainName }).then(() => {
          this.getExecutorList();
          this.$message.successNotify('一键重排操作成功');
        })
        .catch(() => { this.$http.buildErrorHandler('一键重排请求失败！'); });
      });
    },
    batchDelete() {
      this.batchOperation('删除', (arr) => {
        const params = {
          namespace: this.domainName,
          executorNames: arr.join(','),
        };
        this.$message.confirmMessage(`确定删除Executor ${arr.join(',')} 吗?`, () => {
          this.$http.post('/console/executor-overview/remove-executor-batch', params).then(() => {
            this.getExecutorList();
            this.$message.successNotify('批量删除Executor操作成功');
          })
          .catch(() => { this.$http.buildErrorHandler('批量删除Executor请求失败！'); });
        });
      });
    },
    batchOperation(text, callback) {
      if (this.multipleSelection.length <= 0) {
        this.$message.errorMessage(`请选择要批量${text}的Executor！！！`);
      } else {
        const selectedExecutorArray = [];
        this.multipleSelection.forEach((element) => {
          selectedExecutorArray.push(element.executorName);
        });
        callback(selectedExecutorArray);
      }
    },
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    handleReset(row) {
      console.log(row);
    },
    handleDelete(row) {
      const params = {
        namespace: this.domainName,
        executorName: row.executorName,
      };
      this.$message.confirmMessage(`确定删除Executor ${row.executorName} 吗?`, () => {
        this.$http.post('/console/executor-overview/remove-executor', params).then(() => {
          this.getExecutorList();
          this.$message.successNotify('删除Executor操作成功');
        })
        .catch(() => { this.$http.buildErrorHandler('删除Executor请求失败！'); });
      });
    },
    handleTraffic(row, operation) {
      const params = {
        namespace: this.domainName,
        executorName: row.executorName,
        operation,
      };
      let text = '';
      if (operation === 'extract') {
        text = '摘取';
      } else {
        text = '恢复';
      }
      this.$message.confirmMessage(`确认${text}Executor ${row.executorName} 流量吗?`, () => {
        this.$http.post('/console/executor-overview/traffic', params).then(() => {
          this.getExecutorList();
          this.$message.successNotify(`${text}流量操作成功`);
        })
        .catch(() => { this.$http.buildErrorHandler(`${text}流量请求失败！`); });
      });
    },
    getExecutorList() {
      this.loading = true;
      this.$http.get('/console/executor-overview/executors', { namespace: this.domainName }).then((data) => {
        this.executorList = data;
        this.total = data.length;
      })
      .catch(() => { this.$http.buildErrorHandler('获取executors列表失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
  },
  created() {
    this.getExecutorList();
  },
  components: {
    'executor-allocation-dialog': executorAllocationDialog,
  },
};
</script>
<style lang="sass" scoped>
</style>
