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
                        </el-form-item>
                    </el-form>
                    <div class="page-table" v-loading="loading" element-loading-text="请稍等···">
                        <div class="page-table-header">
                            <div class="page-table-header-title"><i class="fa fa-list"></i>Executor列表
                                <el-button type="text" @click="getExecutorList"><i class="fa fa-refresh"></i></el-button>
                            </div>
                            <div class="page-table-header-separator"></div>
                            <div>
                                <el-button @click="batchReArrange()"><i class="fa fa-repeat text-btn"></i>重排</el-button>
                                <el-button @click="batchPickTraffic()"><i class="fa fa-hand-lizard-o text-btn"></i>摘流量</el-button>
                                <el-button @click="batchReset()"><i class="fa fa-power-off text-btn"></i>重启</el-button>
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
                            <el-table-column label="已分配分片分布" header-align="left" align="center">
                                <template slot-scope="scope">
                                    <el-tooltip content="查看" placement="top">
                                        <el-button type="text" @click=""><i class="fa fa-search-plus"></i></el-button>
                                    </el-tooltip>
                                </template>
                            </el-table-column>
                            <el-table-column prop="version" label="版本"></el-table-column>
                            <el-table-column prop="lastBeginTime" label="启动时间" min-width="90px"></el-table-column>
                            <el-table-column label="操作" width="100px">
                                <template slot-scope="scope">
                                    <el-tooltip content="启用" placement="top">
                                        <el-button type="text" @click="handleEnabled(scope.row)"><i class="fa fa-play-circle"></i></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="编辑" placement="top">
                                        <el-button type="text" icon="el-icon-edit" @click="handleEdit(scope.row)"></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="删除" placement="top">
                                        <el-button type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"></el-button>
                                    </el-tooltip>
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
      domainName: this.$route.params.domain,
      loading: false,
      executorList: [],
      filters: {
        executorName: '',
      },
      orderBy: 'executorName',
      total: 0,
      multipleSelection: [],
    };
  },
  methods: {
    batchReArrange() {
      this.batchOperation('重排', (arr) => {
        console.log(arr);
      });
    },
    batchPickTraffic() {
      this.batchOperation('摘流量', (arr) => {
        console.log(arr);
      });
    },
    batchDelete() {
      this.batchOperation('删除', (arr) => {
        console.log(arr);
      });
    },
    batchReset() {
      this.batchOperation('重启', (arr) => {
        console.log(arr);
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
    handleEdit(row) {
      console.log(row);
    },
    handleDelete(row) {
      console.log(row);
    },
    handleEnabled(row) {
      console.log(row);
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
};
</script>
<style lang="sass" scoped>
</style>
