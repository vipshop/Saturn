<template>
    <div>
        <div>
            <el-row :gutter="20">
                <el-col :span="8">
                    <Panel type="success">
                        <div slot="title">在线数/总数</div>
                        <div slot="content">1/2</div>
                    </Panel>
                </el-col>
            </el-row>
        </div>
        <div class="page-container">
            <el-form :inline="true" class="demo-form-inline">
                <el-form-item label="">
                    <el-input placeholder="搜索" v-model="executorForSearch" size="small"></el-input>
                </el-form-item>
                <el-form-item class="filter-search">
                    <el-button type="primary" icon="el-icon-search" size="small">查询</el-button>
                </el-form-item>
            </el-form>
            <div class="page-table">
                <div class="page-table-header">
                    <div class="page-table-header-title"><i class="fa fa-list"></i>Executor列表
                        <el-button type="text" @click="handleRefresh()"><i class="fa fa-refresh"></i></el-button>
                    </div>
                    <div class="page-separator"></div>
                    <div>
                        <el-button @click="batchReArrange()"><i class="fa fa-repeat text-danger"></i>重排</el-button>
                        <el-button @click="batchPickTraffic()"><i class="fa fa-hand-lizard-o text-danger"></i>摘流量</el-button>
                        <el-button @click="batchReset()"><i class="fa fa-power-off text-danger"></i>重启</el-button>
                        <el-button @click="batchDelete()"><i class="fa fa-trash text-danger"></i>删除</el-button>
                    </div>
                </div>
                <el-table border ref="multipleTable" @selection-change="handleSelectionChange" :data="tableData" style="width: 100%">
                    <el-table-column type="selection" width="55"></el-table-column>
                    <el-table-column prop="name" label="Executor"></el-table-column>
                    <el-table-column label="状态">
                        <template slot-scope="scope"> 
                            <el-tag :type="scope.row.status ? '' : 'danger'" close-transition>{{scope.row.status}}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="ip" label="IP"></el-table-column>
                    <el-table-column prop="frag" label="已分配分片分布"></el-table-column>
                    <el-table-column prop="load" label="负荷"></el-table-column>
                    <el-table-column prop="version" label="版本"></el-table-column>
                    <el-table-column prop="setupTime" label="启动时间"></el-table-column>
                    <el-table-column label="操作">
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
                <el-pagination
                    @size-change="handleSizeChange"
                    @current-change="changePage"
                    :current-page="currentPage"
                    :page-sizes="[10, 20, 50, 100]"
                    :page-size="PageSize"
                    layout="prev, pager, next, total, sizes"
                    :total="total">
                </el-pagination>
            </div>
        </div>
    </div>
</template>

<script>
export default {
  data() {
    return {
      executorForSearch: '',
      tableData: [{
        id: 1,
        name: '123',
        status: true,
        ip: '192.168.0.1',
        frag: '123:2',
        load: 2,
        version: '2.1.6',
        setupTime: '',
      }, {
        id: 2,
        name: '123',
        status: false,
        ip: '192.168.0.1',
        frag: '123:2',
        load: 2,
        version: '2.1.6',
        setupTime: '',
      }],
      currentPage: 1,
      PageSize: 10,
      total: 11,
      multipleSelection: [],
    };
  },
  methods: {
    batchReArrange() {
      this.batchOperation('重排', (data) => {
        console.log(data);
      });
    },
    batchPickTraffic() {
      this.batchOperation('摘流量', (data) => {
        console.log(data);
      });
    },
    batchDelete() {
      this.batchOperation('删除', (data) => {
        console.log(data);
      });
    },
    batchReset() {
      this.batchOperation('重启', (data) => {
        console.log(data);
      });
    },
    batchOperation(text, callback) {
      if (this.multipleSelection.length <= 0) {
        this.$message.errorMessage(`请选择要批量${text}的Executor！！！`);
      } else {
        const selectedJobArray = [];
        const selectedJobNameArray = [];
        this.multipleSelection.forEach((element) => {
          selectedJobArray.push(element.id);
          selectedJobNameArray.push(element.name);
        });
        const selectedJobStr = selectedJobArray.join(',');
        const selectedJobNameStr = selectedJobNameArray.join(' ; ');
        this.$message.confirmMessage(`确认${text}Executor ${selectedJobNameStr} 吗?`, () => {
          callback(selectedJobStr);
        });
      }
    },
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    changePage(currentPage) {
      this.currentPage = currentPage;
      console.log(this.currentPage);
    },
    handleSizeChange(val) {
      console.log(`每页 ${val} 条`);
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
  },
};
</script>
<style lang="sass" scoped>
</style>
