<template>
    <div class="margin-20">
        <div>
            <el-row :gutter="20">
                <el-col :span="8">
                    <Panel type="success">
                        <div slot="title">在线数 / 总数</div>
                        <div slot="content">{{onlineNum}} / {{totalNum}}</div>
                    </Panel>
                </el-col>
            </el-row>
        </div>
        <div class="page-container">
            <FilterPageList :data="executorList" :total="total" :order-by="orderBy" :filters="filters">
                <template slot-scope="scope">
                    <el-form :inline="true" class="table-filter">
                        <input type="text" v-show="false"/>
                        <el-form-item label="">
                            <el-input placeholder="搜索" v-model="filters.executorName" @keyup.enter.native="scope.search"></el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" icon="el-icon-search" @click="scope.search">查询</el-button>
                            <el-button type="primary" v-if="$common.hasPerm('executor:shardAllAtOnce', domainName)" @click="handleReArrange()"><i class="fa fa-repeat"></i>一键重排</el-button>
                        </el-form-item>
                    </el-form>
                    <div class="page-table" v-loading="loading" element-loading-text="请稍等···">
                        <div class="page-table-header">
                            <div class="page-table-header-title"><i class="fa fa-list"></i>Executor列表
                                <el-button type="text" @click="getExecutorList"><i class="fa fa-refresh"></i></el-button>
                            </div>
                            <div class="page-table-header-separator"></div>
                            <div>
                                <el-button @click="batchDelete()" v-if="$common.hasPerm('executor:batchRemove', domainName)"><i class="fa fa-trash text-danger"></i>删除</el-button>
                            </div>
                        </div>
                        <el-table stripe border ref="multipleTable" @selection-change="handleSelectionChange" @sort-change="scope.onSortChange" :data="scope.pageData" style="width: 100%">
                            <el-table-column type="selection" width="55"></el-table-column>
                            <el-table-column prop="executorName" label="Executor" min-width="160px" show-overflow-tooltip sortable>
                                <template slot-scope="scope">
                                    <i class="iconfont icon-docker" v-if="scope.row.container"></i>
                                    <i class="iconfont icon-zhuji" v-if="!scope.row.container"></i>
                                    {{scope.row.executorName}}
                                </template>
                            </el-table-column>
                            <el-table-column prop="status" label="状态" width="80px" sortable>
                                <template slot-scope="scope"> 
                                    <el-tag :type="scope.row.status === 'ONLINE' ? 'success' : ''" close-transition>{{translateStatus[scope.row.status]}}</el-tag>
                                    <el-tag type="warning" v-if="scope.row.restarting">重启中</el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column prop="serverIp" label="IP" width="120"></el-table-column>
                            <el-table-column prop="groupName" label="分组" show-overflow-tooltip sortable>
                                <template slot-scope="scope">
                                    {{scope.row.groupName || '--'}}
                                </template>
                            </el-table-column>
                            <el-table-column label="分片分配" header-align="left" width="80px" align="center">
                                <template slot-scope="scope">
                                    <span v-if="scope.row.status === 'OFFLINE'">--</span>
                                    <el-tooltip content="查看" placement="top" v-else>
                                        <el-button type="text" @click="getExecutorAllocation(scope.row)"><i class="fa fa-search-plus"></i></el-button>
                                    </el-tooltip>
                                </template>
                            </el-table-column>
                            <el-table-column prop="version" label="版本" sortable></el-table-column>
                            <el-table-column prop="lastBeginTime" label="最近启动时间" width="160px"></el-table-column>
                            <el-table-column label="操作" width="120px" align="center">
                                <template slot-scope="scope">
                                    <el-tooltip content="重启" placement="top" v-if="$common.hasPerm('executor:restart', domainName) && (scope.row.status === 'ONLINE' && !scope.row.restarting && isAbledDump(scope.row.version))">
                                        <el-button type="text" @click="handleRestart(scope.row)"><i class="fa fa-power-off"></i></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="摘取流量" placement="top" v-if="$common.hasPerm('executor:extractOrRecoverTraffic', domainName) && !scope.row.noTraffic">
                                        <el-button type="text" @click="handleTraffic(scope.row, 'extract')"><i class="fa fa-stop-circle text-warning"></i></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="恢复流量" placement="top" v-if="$common.hasPerm('executor:extractOrRecoverTraffic', domainName) && scope.row.noTraffic">
                                        <el-button type="text" @click="handleTraffic(scope.row, 'recover')"><i class="fa fa-play-circle"></i></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="一键DUMP" placement="top" v-if="scope.row.status === 'ONLINE'">
                                        <el-button type="text" @click="$common.handleDump(scope.row, domainName, dumpNext.bind(this))"><i class="fa fa-database"></i></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="删除" placement="top" v-if="$common.hasPerm('executor:remove', domainName) && scope.row.status === 'OFFLINE'">
                                        <el-button type="text" icon="el-icon-delete text-danger" @click="handleDelete(scope.row)"></el-button>
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
      loading: false,
      executorList: [],
      filters: {
        executorName: '',
      },
      translateStatus: {
        ONLINE: '在线',
        OFFLINE: '离线',
      },
      orderBy: '-status',
      total: 0,
      multipleSelection: [],
      isExecutorAllocationVisible: false,
      executorAllocationInfo: {},
    };
  },
  methods: {
    dumpNext() {
      this.getExecutorList();
      this.$message.successNotify('一键DUMP操作成功');
    },
    handleRestart(row) {
      this.$message.confirmMessage(`确定重启 ${row.executorName} 吗?`, () => {
        this.$http.post(`/console/namespaces/${this.domainName}/executors/${row.executorName}/restart`, '').then(() => {
          this.getExecutorList();
          this.$message.successNotify('重启操作成功');
        })
        .catch(() => { this.$http.buildErrorHandler('重启executor请求失败！'); });
      });
    },
    getExecutorAllocation(row) {
      this.$http.get(`/console/namespaces/${this.domainName}/executors/${row.executorName}/allocation`).then((data) => {
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
        this.$http.post(`/console/namespaces/${this.domainName}/executors/shardAll`, '').then(() => {
          this.getExecutorList();
          this.$message.successNotify('一键重排操作成功');
        })
        .catch(() => { this.$http.buildErrorHandler('一键重排请求失败！'); });
      });
    },
    batchDelete() {
      this.batchOperation('删除', (arr) => {
        const offlineExecutors = this.getOfflineExecutorArray(arr);
        if (offlineExecutors.length === 0) {
          this.$message.errorMessage('没有可以删除的Executor,请重新勾选!');
        } else {
          const params = {
            executorNames: this.getExecutorNameArray(offlineExecutors).join(','),
          };
          const confirmText = offlineExecutors.length < 10 ? `确定删除Executor ${params.executorNames} 吗?` : `确认删除已选的 ${offlineExecutors.length} 条Executor吗?`;
          this.$message.confirmMessage(confirmText, () => {
            this.$http.delete(`/console/namespaces/${this.domainName}/executors`, params).then(() => {
              this.getExecutorList();
              this.$message.successNotify('批量删除Executor操作成功');
            })
            .catch(() => { this.$http.buildErrorHandler('批量删除Executor请求失败！'); });
          });
        }
      });
    },
    batchOperation(text, callback) {
      if (this.multipleSelection.length <= 0) {
        this.$message.errorMessage(`请选择要批量${text}的Executor！！！`);
      } else {
        const selectedExecutorArray = [];
        this.multipleSelection.forEach((element) => {
          selectedExecutorArray.push(element);
        });
        callback(selectedExecutorArray);
      }
    },
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    handleDelete(row) {
      this.$message.confirmMessage(`确定删除Executor ${row.executorName} 吗?`, () => {
        this.$http.delete(`/console/namespaces/${this.domainName}/executors/${row.executorName}`).then(() => {
          this.getExecutorList();
          this.$message.successNotify('删除Executor操作成功');
        })
        .catch(() => { this.$http.buildErrorHandler('删除Executor请求失败！'); });
      });
    },
    handleTraffic(row, operation) {
      const params = {
        operation,
      };
      let text = '';
      if (operation === 'extract') {
        text = '摘取';
      } else {
        text = '恢复';
      }
      this.$message.confirmMessage(`确认${text}Executor ${row.executorName} 流量吗?`, () => {
        this.$http.post(`/console/namespaces/${this.domainName}/executors/${row.executorName}/traffic`, params).then(() => {
          this.getExecutorList();
          this.$message.successNotify(`${text}流量操作成功`);
        })
        .catch(() => { this.$http.buildErrorHandler(`${text}流量请求失败！`); });
      });
    },
    getExecutorNameArray(arr) {
      const executorNameArray = [];
      arr.forEach((ele) => {
        executorNameArray.push(ele.executorName);
      });
      return executorNameArray;
    },
    getOfflineExecutorArray(arr) {
      const resultArr = [];
      arr.forEach((ele) => {
        if (ele.status === 'OFFLINE') {
          resultArr.push(ele);
        }
      });
      return resultArr;
    },
    getExecutorList() {
      this.loading = true;
      this.$http.get(`/console/namespaces/${this.domainName}/executors`).then((data) => {
        this.executorList = data;
        this.total = data.length;
      })
      .catch(() => { this.$http.buildErrorHandler('获取executors列表失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    isAbledDump(str) {
      let flag = false;
      if (str) {
        const first = Number(str[0]);
        if (Number.isInteger(first)) {
          if (first >= 3) {
            flag = true;
          }
        } else {
          flag = true;
        }
      }
      return flag;
    },
  },
  computed: {
    onlineNum() {
      const num = [];
      this.executorList.forEach((ele) => {
        if (ele.status === 'ONLINE') {
          num.push(ele);
        }
      });
      return num.length;
    },
    totalNum() {
      return this.executorList.length;
    },
    domainName() {
      return this.$route.params.domain;
    },
  },
  created() {
    this.getExecutorList();
  },
  components: {
    'executor-allocation-dialog': executorAllocationDialog,
  },
  watch: {
    $route: 'getExecutorList',
  },
};
</script>
<style lang="sass" scoped>
</style>
