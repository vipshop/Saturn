<template>
    <div>
        <div>
            <el-row :gutter="20">
                <el-col :span="8">
                    <Panel type="success">
                        <div slot="title">启用作业数 / 总作业数</div>
                        <div slot="content">{{enabledNumber}} / {{totalNumber}}</div>
                    </Panel>
                </el-col>
                <el-col :span="8">
                    <Panel type="danger">
                        <div slot="title">异常作业数</div>
                        <div slot="content">{{abnormalNumber}}</div>
                    </Panel>
                </el-col>
            </el-row>
        </div>
        <div class="page-container">
            <FilterPageList :data="jobList" :total="total" :order-by="orderBy" :filters="filters">
                <template slot-scope="scope">
                    <el-form :inline="true" class="table-filter">
                        <el-form-item label="">
                            <el-select v-model="filters.groups" @change="scope.search">
                                <el-option label="全部分组" value=""></el-option>
                                <el-option v-for="item in groupList" :label="item" :value="item" :key="item"></el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="">
                            <el-input placeholder="搜索" v-model="filters.jobName"></el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" icon="el-icon-search" @click="scope.search">查询</el-button>
                        </el-form-item>
                    </el-form>
                    <div class="page-table" v-loading="loading" element-loading-text="请稍等···">
                        <div class="page-table-header">
                            <div class="page-table-header-title"><i class="fa fa-list"></i>作业列表
                                <el-button type="text" @click="getJobList"><i class="fa fa-refresh"></i></el-button>
                            </div>
                            <div class="page-table-header-separator"></div>
                            <div>
                                <el-button @click="batchEnabled()"><i class="fa fa-play-circle text-btn"></i>启用</el-button>
                                <el-button @click="batchDisabled()"><i class="fa fa-stop-circle text-btn"></i>禁用</el-button>
                                <el-button @click="batchDelete()"><i class="fa fa-trash text-btn"></i>删除</el-button>
                                <el-button @click="batchPriority()"><i class="fa fa-level-up text-btn"></i>优先</el-button>
                            </div>
                            <div class="pull-right">
                                <el-button @click="handleAdd()"><i class="fa fa-plus-circle text-btn"></i>添加</el-button>
                                <el-button @click="handleImport()"><i class="fa fa-arrow-circle-o-down text-btn"></i>导入</el-button>
                                <el-button @click="handleExport()"><i class="fa fa-arrow-circle-o-up text-btn"></i>导出</el-button>
                            </div>
                        </div>
                        <el-table stripe border ref="multipleTable" @selection-change="handleSelectionChange" @sort-change="scope.onSortChange" :data="scope.pageData" style="width: 100%">
                            <el-table-column type="selection" width="55"></el-table-column>
                            <el-table-column prop="jobName" label="作业名" sortable>
                                <template slot-scope="scope">
                                    <router-link tag="a" :to="{ name: 'job_setting', params: { domain: domainName, jobName: scope.row.jobName } }">
                                        <el-button type="text">
                                          <i class="iconfont icon-java" v-if="scope.row.jobType === 'JAVA_JOB'"></i>
                                          <i class="iconfont icon-shell" v-if="scope.row.jobType === 'SHELL_JOB'"></i>
                                          {{scope.row.jobName}}
                                        </el-button>
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
                            <el-table-column label="分片情况">
                                <template slot-scope="scope">
                                    <el-tooltip placement="right" :disabled="$array.strToArray(scope.row.shardingList).length === 0">
                                        <el-tag>{{$array.strToArray(scope.row.shardingList).length}} Executor(s)</el-tag>
                                        <div slot="content" v-for="item in $array.strToArray(scope.row.shardingList)" :key="item">
                                            <div>{{item}}</div>
                                        </div>
                                    </el-tooltip>
                                </template>
                            </el-table-column>
                            <el-table-column label="操作" width="100px" align="center">
                                <template slot-scope="scope">
                                    <el-tooltip content="启用" placement="top" v-if="scope.row.status === 'STOPPING' || scope.row.status === 'STOPPED'">
                                        <el-button type="text" @click="handleActive(scope.row, true)"><i class="fa fa-play-circle"></i></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="禁用" placement="top" v-if="scope.row.status === 'READY' || scope.row.status === 'RUNNING'">
                                        <el-button type="text" @click="handleActive(scope.row, false)"><i class="fa fa-stop-circle"></i></el-button>
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
            <div v-if="isJobInfoVisible">
                <job-info-dialog :domain-name="domainName" :job-info="jobInfo" :job-info-title="jobInfoTitle" :job-info-operation="jobInfoOperation" @job-info-success="jobInfoSuccess" @close-dialog="closeDialog"></job-info-dialog>
            </div>
            <div v-if="isBatchPriorityVisible">
                <batch-priority-dialog :job-names-array="jobNamesArray" :domain-name="domainName" @close-dialog="closePriorityDialog" @batch-priority-success="batchPrioritySuccess"></batch-priority-dialog>
            </div>
            <div v-if="isImportVisible">
                <ImportFileDialog :import-data="importData" import-title="导入作业" @close-dialog="closeImportDialog" @import-success="importSuccess"></ImportFileDialog>
            </div>
        </div>
    </div>
</template>

<script>
import jobInfoDialog from './job_info_dialog';
import batchPriorityDialog from './batch_priority_dialog';

export default {
  data() {
    return {
      domainName: this.$route.params.domain,
      loading: false,
      isJobInfoVisible: false,
      jobInfoTitle: '',
      jobInfoOperation: '',
      jobInfo: {},
      isBatchPriorityVisible: false,
      isImportVisible: false,
      importData: {
        namespace: this.$route.params.domain,
      },
      jobNamesArray: [],
      filters: {
        jobName: '',
        groups: '',
      },
      orderBy: 'jobName',
      groupList: [],
      jobList: [],
      statusTag: {
        READY: 'primary',
        RUNNING: 'success',
        STOPPING: 'warning',
        STOPPED: 'warning',
      },
      total: 0,
      multipleSelection: [],
      abnormalNumber: 0,
      enabledNumber: 0,
      totalNumber: 0,
    };
  },
  methods: {
    handleExport() {
      window.location.href = `/console/job-overview/export-jobs?namespace=${this.domainName}`;
    },
    handleImport() {
      this.isImportVisible = true;
    },
    closeImportDialog() {
      this.isImportVisible = false;
    },
    importSuccess() {
      this.isImportVisible = false;
      this.getJobList();
      this.$message.successNotify('导入作业操作成功');
    },
    handleAdd() {
      this.isJobInfoVisible = true;
      this.jobInfoTitle = '添加作业';
      this.jobInfoOperation = 'add';
      const jobAddInfo = {
        jobType: 'JAVA_JOB',
        jobName: '',
        jobClass: '',
        cron: '0/5 * * * * ?',
        shardingTotalCount: 1,
        shardingItemParameters: '',
        description: '',
      };
      this.jobInfo = JSON.parse(JSON.stringify(jobAddInfo));
    },
    closeDialog() {
      this.isJobInfoVisible = false;
    },
    jobInfoSuccess() {
      this.isJobInfoVisible = false;
      this.getJobList();
      this.$message.successNotify('保存作业操作成功');
    },
    batchEnabled() {
      this.batchOperation('启用', (arr) => {
        const params = {
          namespace: this.domainName,
          jobNames: arr.join(','),
        };
        this.handleBatchActive(params, arr, true);
      });
    },
    batchDisabled() {
      this.batchOperation('禁用', (arr) => {
        const params = {
          namespace: this.domainName,
          jobNames: arr.join(','),
        };
        this.handleBatchActive(params, arr, false);
      });
    },
    batchDelete() {
      this.batchOperation('删除', (arr) => {
        const params = {
          namespace: this.domainName,
          jobNames: arr.join(','),
        };
        this.$message.confirmMessage(`确认删除作业 ${params.jobNames} 吗?`, () => {
          this.$http.post('/console/job-overview/remove-job-batch', params).then(() => {
            this.getJobList();
            this.$message.successNotify('批量删除作业操作成功');
          })
          .catch(() => { this.$http.buildErrorHandler('批量删除作业请求失败！'); });
        });
      });
    },
    batchPriority() {
      this.batchOperation('优先Executors', (arr) => {
        this.jobNamesArray = JSON.parse(JSON.stringify(arr));
        this.isBatchPriorityVisible = true;
      });
    },
    closePriorityDialog() {
      this.isBatchPriorityVisible = false;
    },
    batchPrioritySuccess() {
      this.isBatchPriorityVisible = false;
      this.getJobList();
      this.$message.successNotify('批量设置作业的优先Executors成功');
    },
    batchOperation(text, callback) {
      if (this.multipleSelection.length <= 0) {
        this.$message.errorMessage(`请选择要批量${text}的作业！！！`);
      } else {
        const selectedJobNameArray = [];
        this.multipleSelection.forEach((element) => {
          selectedJobNameArray.push(element.jobName);
        });
        callback(selectedJobNameArray);
      }
    },
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    handleEdit(row) {
      const params = {
        namespace: this.domainName,
        jobName: row.jobName,
      };
      this.$http.get('/console/job-overview/job-config', params).then((data) => {
        const jobEditInfo = {
          jobType: data.jobType,
          jobName: data.jobName,
          jobClass: data.jobClass,
          cron: data.cron,
          shardingTotalCount: data.shardingTotalCount,
          shardingItemParameters: data.shardingItemParameters,
          description: data.description,
        };
        this.isJobInfoVisible = true;
        this.jobInfoTitle = '编辑作业';
        this.jobInfoOperation = 'edit';
        this.jobInfo = JSON.parse(JSON.stringify(jobEditInfo));
      })
      .catch(() => { this.$http.buildErrorHandler('获取作业信息请求失败！'); });
    },
    handleDelete(row) {
      const params = {
        namespace: this.domainName,
        jobName: row.jobName,
      };
      this.$message.confirmMessage(`确认删除作业 ${row.jobName} 吗?`, () => {
        this.$http.post('/console/job-overview/remove-job', params).then(() => {
          this.getJobList();
          this.$message.successNotify('删除作业操作成功');
        })
        .catch(() => { this.$http.buildErrorHandler('删除作业请求失败！'); });
      });
    },
    handleBatchActive(params, jobArray, enabled) {
      let dependUrl = '';
      let operation = '';
      let text = '';
      let activeRequest = '';
      if (enabled) {
        dependUrl = '/console/job-overview/depending-jobs-batch';
        operation = '启用';
        text = '禁用';
        activeRequest = 'enable-job-batch';
      } else {
        dependUrl = '/console/job-overview/depended-jobs-batch';
        operation = '禁用';
        text = '启用';
        activeRequest = 'disable-job-batch';
      }
      this.$http.get(dependUrl, params).then((data) => {
        let warningFlag = false;
        jobArray.forEach((ele) => {
          if (data[ele].length > 0) {
            if (enabled) {
              warningFlag = data[ele].some((ele2) => {
                if (!ele2.enabled) {
                  return true;
                }
                return false;
              });
            } else {
              warningFlag = data[ele].some((ele3) => {
                if (ele3.enabled) {
                  return true;
                }
                return false;
              });
            }
          }
          return false;
        });
        if (warningFlag) {
          this.$message.confirmMessage(`有依赖的作业已${text}，是否继续${operation}作业?`, () => {
            this.enabledRequest(params, activeRequest);
          });
        } else {
          this.$message.confirmMessage(`确定${operation}作业${params.jobNames}吗?`, () => {
            this.enabledRequest(params, activeRequest);
          });
        }
      })
      .catch(() => { this.$http.buildErrorHandler(`${dependUrl}请求失败！`); });
    },
    handleActive(row, enabled) {
      let dependUrl = '';
      let operation = '';
      let text = '';
      let activeRequest = '';
      if (enabled) {
        dependUrl = '/console/job-overview/depending-jobs';
        operation = '启用';
        text = '禁用';
        activeRequest = 'enable-job';
      } else {
        dependUrl = '/console/job-overview/depended-jobs';
        operation = '禁用';
        text = '启用';
        activeRequest = 'disable-job';
      }
      const params = {
        namespace: this.domainName,
        jobName: row.jobName,
      };
      this.$http.get(dependUrl, params).then((data) => {
        const arr = data;
        if (arr.length > 0) {
          const jobArr = [];
          if (enabled) {
            arr.forEach((ele) => {
              if (!ele.enabled) {
                jobArr.push(ele.jobName);
              }
            });
          } else {
            arr.forEach((ele) => {
              if (ele.enabled) {
                jobArr.push(ele.jobName);
              }
            });
          }
          if (jobArr.length > 0) {
            const jobStr = jobArr.join(',');
            this.$message.confirmMessage(`有依赖的作业${jobStr}已${text}，是否继续${operation}该作业?`, () => {
              this.enabledRequest(params, activeRequest);
            });
          } else {
            this.$message.confirmMessage(`确定${operation}作业${row.jobName}吗?`, () => {
              this.enabledRequest(params, activeRequest);
            });
          }
        } else {
          this.$message.confirmMessage(`确定${operation}作业${row.jobName}吗?`, () => {
            this.enabledRequest(params, activeRequest);
          });
        }
      })
      .catch(() => { this.$http.buildErrorHandler(`${dependUrl}请求失败！`); });
    },
    enabledRequest(params, reqUrl) {
      this.loading = true;
      this.$http.post(`/console/job-overview/${reqUrl}`, params).then(() => {
        this.$message.successNotify('操作成功');
        this.getJobList();
      })
      .catch(() => { this.$http.buildErrorHandler(`${reqUrl}请求失败！`); })
      .finally(() => {
        this.loading = false;
      });
    },
    getJobList() {
      this.loading = true;
      this.$http.get('/console/job-overview/jobs', { namespace: this.domainName }).then((data) => {
        this.jobList = data.jobs;
        this.totalNumber = data.totalNumber;
        this.enabledNumber = data.enabledNumber;
        this.abnormalNumber = data.abnormalNumber;
        this.total = data.length;
      })
      .catch(() => { this.$http.buildErrorHandler('获取作业列表失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    getGroupList() {
      this.loading = true;
      this.$http.get('/console/job-overview/groups', { namespace: this.domainName }).then((data) => {
        this.groupList = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取groups失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
  },
  components: {
    'job-info-dialog': jobInfoDialog,
    'batch-priority-dialog': batchPriorityDialog,
  },
  created() {
    this.getJobList();
    this.getGroupList();
  },
};
</script>
<style lang="sass" scoped>
.devicon-java-plain:before {
    content: "\F144";
}
</style>
