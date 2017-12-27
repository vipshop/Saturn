<template>
    <div>
        <div>
            <el-row :gutter="20">
                <el-col :span="8">
                    <Panel type="default">
                        <div slot="title">启用作业数/总作业数</div>
                        <div slot="content">2/3</div>
                    </Panel>
                </el-col>
                <el-col :span="8">
                    <Panel type="warning">
                        <div slot="title">失败率</div>
                        <div slot="content">1%</div>
                    </Panel>
                </el-col>
                <el-col :span="8">
                    <Panel type="danger">
                        <div slot="title">异常作业数</div>
                        <div slot="content">0</div>
                    </Panel>
                </el-col>
            </el-row>
        </div>
        <div class="page-container">
            <FilterPageList :data="jobList" :total="total" :order-by="orderBy" :filters="filters">
                <template slot-scope="scope">
                    <el-form :inline="true" class="demo-form-inline">
                        <el-form-item label="">
                            <el-select v-model="filters.groups" @change="scope.search">
                                <el-option label="全部分组" value=""></el-option>
                                <el-option v-for="item in groupList" :label="item" :value="item" :key="item"></el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="">
                            <el-input placeholder="搜索" v-model="filters.jobName"></el-input>
                        </el-form-item>
                        <el-form-item class="filter-search">
                            <el-button type="primary" icon="el-icon-search" @click="scope.search">查询</el-button>
                        </el-form-item>
                    </el-form>
                    <div class="page-table">
                        <div class="page-table-header">
                            <div class="page-table-header-title"><i class="fa fa-list"></i>作业列表
                                <el-button type="text" @click="handleRefresh()"><i class="fa fa-refresh"></i></el-button>
                            </div>
                            <div class="page-table-header-separator"></div>
                            <div>
                                <el-button @click="batchEnabled()"><i class="fa fa-play-circle text-danger"></i>启用</el-button>
                                <el-button @click="batchStop()"><i class="fa fa-stop-circle text-danger"></i>禁用</el-button>
                                <el-button @click="batchDelete()"><i class="fa fa-trash text-danger"></i>删除</el-button>
                                <el-button @click="batchPriority()"><i class="fa fa-level-up text-danger"></i>优先</el-button>
                            </div>
                            <div class="pull-right">
                                <el-button @click="handleAdd()"><i class="fa fa-plus-circle text-danger"></i>添加</el-button>
                                <el-button @click="handleImport()"><i class="fa fa-arrow-circle-o-down text-danger"></i>导入</el-button>
                                <el-button @click="handleExport()"><i class="fa fa-arrow-circle-o-up text-danger"></i>导出</el-button>
                            </div>
                        </div>
                        <el-table stripe border ref="multipleTable" @selection-change="handleSelectionChange" @sort-change="scope.onSortChange" :data="scope.pageData" style="width: 100%">
                            <el-table-column type="selection" width="55"></el-table-column>
                            <el-table-column prop="jobName" label="作业名" sortable>
                                <template slot-scope="scope">
                                    <router-link tag="a" :to="{ name: 'job_setting', params: { domain: domainName, jobName: scope.row.jobName } }">
                                        <el-button type="text">{{scope.row.jobName}}</el-button>
                                    </router-link>
                                </template>
                            </el-table-column>
                            <el-table-column label="状态">
                                <template slot-scope="scope"> 
                                    <el-tag :type="statusTag[scope.row.status]" close-transition>{{scope.row.status}}</el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column prop="groups" label="分组"></el-table-column>
                            <el-table-column prop="shardingTotalCount" label="分片数"></el-table-column>
                            <el-table-column label="分片分布">
                                <template slot-scope="scope">
                                    <el-tooltip placement="right" :disabled="$array.strToArray(scope.row.shardingList).length === 0">
                                        <el-tag>{{$array.strToArray(scope.row.shardingList).length}}</el-tag>
                                        <div slot="content" v-for="item in $array.strToArray(scope.row.shardingList)" :key="item">
                                            <div>{{item}}</div>
                                        </div>
                                    </el-tooltip>
                                </template>
                            </el-table-column>
                            <el-table-column label="操作" width="100px">
                                <template slot-scope="scope">
                                    <el-tooltip content="启用" placement="top" v-if="scope.row.status === 'READY' || scope.row.status === 'STOPPED'">
                                        <el-button type="text" @click="handleEnabled(scope.row)"><i class="fa fa-play-circle"></i></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="禁用" placement="top" v-if="scope.row.status === 'RUNNING'">
                                        <el-button type="text" @click="handleEnabled(scope.row)"><i class="fa fa-stop-circle"></i></el-button>
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
      filters: {
        jobName: '',
        groups: '',
      },
      orderBy: 'jobName',
      groupList: ['group1', 'group2'],
      jobList: [],
      statusTag: {
        READY: '',
        RUNNING: 'success',
        STOPPING: 'warning',
        STOPPED: 'danger',
      },
      total: 0,
      multipleSelection: [],
    };
  },
  methods: {
    batchEnabled() {
      this.batchOperation('启用', (data) => {
        console.log(data);
      });
    },
    batchStop() {
      this.batchOperation('停用', (data) => {
        console.log(data);
      });
    },
    batchDelete() {
      this.batchOperation('删除', (data) => {
        console.log(data);
      });
    },
    batchPriority() {
      this.batchOperation('优先', (data) => {
        console.log(data);
      });
    },
    batchOperation(text, callback) {
      if (this.multipleSelection.length <= 0) {
        this.$message.errorMessage(`请选择要批量${text}的作业！！！`);
      } else {
        const selectedJobNameArray = [];
        this.multipleSelection.forEach((element) => {
          selectedJobNameArray.push(element.jobName);
        });
        const selectedJobNameStr = selectedJobNameArray.join(',');
        this.$message.confirmMessage(`确认${text}作业 ${selectedJobNameStr} 吗?`, () => {
          callback(selectedJobNameStr);
        });
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
    getJobList() {
      this.$http.getData('/console/job-overview/jobs', { namespace: this.domainName }).then((data) => {
        if (data) {
          this.jobList = data;
          this.total = data.length;
        }
      });
    },
    getGroupList() {
      this.$http.getData('/console/job-overview/groups', { namespace: this.domainName }).then((data) => {
        if (data) {
          this.groupList = data;
        }
      });
    },
  },
  created() {
    this.getJobList();
    this.getGroupList();
  },
};
</script>
<style lang="sass" scoped>
</style>
