<template>
    <div class="margin-20">
        <div>
            <el-row :gutter="20">
                <el-col :span="8">
                    <Panel type="success">
                        <div slot="title">启用作业数 / 总作业数</div>
                        <div slot="content">{{enabledNumber}} / {{totalNumber}}</div>
                    </Panel>
                </el-col>
                <el-col :span="8">
                    <a @click="toAbnormalJobPage">
                        <Panel type="danger">
                            <div slot="title">异常作业数</div>
                            <div slot="content">{{abnormalNumber}}</div>
                        </Panel>
                    </a>
                </el-col>
            </el-row>
        </div>
        <div class="page-container">
            <FilterPageList :data="jobList" :total="total" :order-by="orderBy" :filters="filters">
                <template slot-scope="scope">
                    <el-form :inline="true" class="table-filter">
                        <el-form-item label="">
                            <el-select style="width: 140px;" v-model="filters.groups" @change="scope.search">
                                <el-option label="全部分组" value=""></el-option>
                                <el-option v-for="item in groupList" :label="item" :value="item" :key="item"></el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="">
                            <el-select style="width: 140px;" v-model="filters.status" @change="scope.search">
                                <el-option label="全部状态" value=""></el-option>
                                <el-option v-for="item in $option.jobStatusTypes" :label="item.label" :value="item.value" :key="item.value"></el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="">
                            <el-input :placeholder="filterColumnPlaceholder" v-model="filters[selectColumn]" @keyup.enter.native="scope.search">
                              <el-select style="width: 120px;" slot="prepend" v-model="selectColumn">
                                  <el-option label="作业名" value="jobName"></el-option>
                                  <el-option label="作业描述" value="description"></el-option>
                              </el-select>
                            </el-input>
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
                                <el-button @click="batchEnabled()" v-if="$common.hasPerm('job:batchEnable', domainName)"><i class="fa fa-play-circle text-btn"></i>启用</el-button>
                                <el-button @click="batchDisabled()" v-if="$common.hasPerm('job:batchDisable', domainName)"><i class="fa fa-stop-circle text-warning"></i>禁用</el-button>
                                <el-button @click="batchDelete()" v-if="$common.hasPerm('job:batchRemove', domainName)"><i class="fa fa-trash text-danger"></i>删除</el-button>
                                <el-button @click="batchPriority()" v-if="$common.hasPerm('job:batchSetPreferExecutors', domainName)"><i class="fa fa-level-up text-btn"></i>优先</el-button>
                            </div>
                            <div class="pull-right">
                                <el-button @click="handleAdd()" v-if="$common.hasPerm('job:add', domainName)"><i class="fa fa-plus-circle text-btn"></i>添加</el-button>
                                <el-button @click="handleImport()" v-if="$common.hasPerm('job:import', domainName)"><i class="fa fa-arrow-circle-o-down text-btn"></i>导入</el-button>
                                <el-button @click="handleExport()" v-if="$common.hasPerm('job:export', domainName)"><i class="fa fa-arrow-circle-o-up text-btn"></i>导出</el-button>
                            </div>
                        </div>
                        <el-table stripe border ref="multipleTable" @selection-change="handleSelectionChange" @sort-change="scope.onSortChange" :data="scope.pageData" style="width: 100%">
                            <el-table-column type="selection" width="55"></el-table-column>
                            <el-table-column prop="jobName" label="作业名" sortable>
                                <template slot-scope="scope">
                                    <router-link tag="a" :to="{ name: 'job_setting', params: { domain: domainName, jobName: scope.row.jobName } }">
                                        <el-button type="text">
                                          <i class="iconfont icon-java" v-if="scope.row.jobType === 'JAVA_JOB'"></i>
                                          <i class="iconfont icon-msnui-logo-linux" v-if="scope.row.jobType === 'SHELL_JOB'"></i>
                                          <i class="fa fa-envelope-o" v-if="scope.row.jobType === 'MSG_JOB'"></i>
                                          {{scope.row.jobName}}
                                        </el-button>
                                    </router-link>
                                </template>
                            </el-table-column>
                            <el-table-column label="状态" prop="status" width="100px" sortable>
                                <template slot-scope="scope"> 
                                    <el-tag :type="statusTag[scope.row.status]" close-transition>{{translateStatus[scope.row.status]}}</el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column prop="groups" label="分组" width="120px" sortable></el-table-column>
                            <el-table-column prop="shardingTotalCount" label="分片数" width="100px"></el-table-column>
                            <el-table-column label="分片情况" width="120px">
                                <template slot-scope="scope">
                                    <el-tooltip placement="right" :disabled="$array.strToArray(scope.row.shardingList).length === 0">
                                        <el-tag :type="$array.strToArray(scope.row.shardingList).length === 0 ? '' : 'primary'">{{$array.strToArray(scope.row.shardingList).length}} Executor(s)</el-tag>
                                        <div slot="content" v-for="item in $array.strToArray(scope.row.shardingList)" :key="item">
                                            <div>{{item}}</div>
                                        </div>
                                    </el-tooltip>
                                </template>
                            </el-table-column>
                            <el-table-column prop="description" show-overflow-tooltip label="描述" width="170px">
                                <template slot-scope="scope"> 
                                    {{scope.row.description || '-'}}
                                </template>
                            </el-table-column>
                            <el-table-column label="操作" width="100px" align="center">
                                <template slot-scope="scope">
                                    <el-tooltip content="启用" placement="top" v-if="$common.hasPerm('job:enable', domainName) && (scope.row.status === 'STOPPING' || scope.row.status === 'STOPPED')">
                                        <el-button type="text" @click="handleActive(scope.row, true)"><i class="fa fa-play-circle"></i></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="禁用" placement="top" v-if="$common.hasPerm('job:disable', domainName) && (scope.row.status === 'READY' || scope.row.status === 'RUNNING')">
                                        <el-button type="text" @click="handleActive(scope.row, false)"><i class="fa fa-stop-circle text-warning"></i></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="复制" placement="top" v-if="$common.hasPerm('job:copy', domainName)">
                                        <el-button type="text" @click="handleCopy(scope.row)"><i class="fa fa-clone"></i></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="删除" placement="top" v-if="$common.hasPerm('job:remove', domainName) && scope.row.status === 'STOPPED'">
                                        <el-button type="text" icon="el-icon-delete text-danger" @click="handleDelete(scope.row)"></el-button>
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
                <ImportFileDialog :import-data="importData" import-template-url="/console/static/jobTemplate/download" :import-url="importUrl" import-title="导入作业" @close-dialog="closeImportDialog" @import-success="importSuccess"></ImportFileDialog>
            </div>
            <div v-if="isImportResultVisible">
                <import-result-dialog :import-result="importResult" @close-dialog="closeImportResultDialog"></import-result-dialog>
            </div>
        </div>
    </div>
</template>

<script>
import jobInfoDialog from './job_info_dialog';
import batchPriorityDialog from './batch_priority_dialog';
import importResultDialog from './import_result_dialog';

export default {
  data() {
    return {
      loading: false,
      isJobInfoVisible: false,
      jobInfoTitle: '',
      jobInfoOperation: '',
      jobInfo: {},
      isBatchPriorityVisible: false,
      isImportVisible: false,
      importResult: [],
      isImportResultVisible: false,
      importData: {
        namespace: this.$route.params.domain,
      },
      importUrl: '',
      jobNamesArray: [],
      filters: {
        jobName: '',
        groups: '',
        status: '',
        description: '',
      },
      orderBy: 'jobName',
      groupList: [],
      jobList: [],
      selectColumn: 'jobName',
      statusTag: {
        READY: 'primary',
        RUNNING: 'success',
        STOPPING: 'warning',
        STOPPED: '',
      },
      translateStatus: {
        READY: '已就绪',
        RUNNING: '运行中',
        STOPPING: '停止中',
        STOPPED: '已停止',
      },
      total: 0,
      multipleSelection: [],
      abnormalNumber: 0,
      enabledNumber: 0,
      totalNumber: 0,
    };
  },
  methods: {
    toAbnormalJobPage() {
      this.$router.push({ name: 'namespace_abnormal_jobs', params: { domain: this.domainName } });
    },
    handleExport() {
      window.location.href = `/console/namespaces/${this.domainName}/jobs/export`;
    },
    handleImport() {
      this.isImportVisible = true;
      this.importUrl = `/console/namespaces/${this.domainName}/jobs/import`;
    },
    closeImportDialog() {
      this.isImportVisible = false;
    },
    importSuccess(importResult) {
      this.isImportVisible = false;
      this.importResult = importResult;
      this.isImportResultVisible = true;
    },
    closeImportResultDialog() {
      this.isImportResultVisible = false;
      this.getJobList();
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
        timeZone: 'Asia/Shanghai',
        queueName: '',
        channelName: '',
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
        const stopedJob = this.getStoptedJobArray(arr);
        if (stopedJob.length === 0) {
          this.$message.errorMessage('没有可以启用的作业,请重新勾选!');
        } else {
          const params = {
            jobNames: this.getJobNameArray(stopedJob).join(','),
          };
          this.handleBatchActive(params, this.getJobNameArray(stopedJob), true);
        }
      });
    },
    batchDisabled() {
      this.batchOperation('禁用', (arr) => {
        const unstopedJob = this.getUnstoptedJobArray(arr);
        if (unstopedJob.length === 0) {
          this.$message.errorMessage('没有可以禁用的作业,请重新勾选!');
        } else {
          const params = {
            jobNames: this.getJobNameArray(unstopedJob).join(','),
          };
          this.handleBatchActive(params, this.getJobNameArray(unstopedJob), false);
        }
      });
    },
    batchDelete() {
      this.batchOperation('删除', (arr) => {
        const stopedJob = this.getStoptedJobArray(arr);
        if (stopedJob.length === 0) {
          this.$message.errorMessage('没有可以删除的作业,请重新勾选!');
        } else {
          const params = {
            jobNames: this.getJobNameArray(stopedJob).join(','),
          };
          this.$message.confirmMessage(`确认删除作业 ${params.jobNames} 吗?`, () => {
            this.$http.delete(`/console/namespaces/${this.domainName}/jobs`, params).then(() => {
              this.getJobList();
              this.$message.successNotify('批量删除作业操作成功');
            })
            .catch(() => { this.$http.buildErrorHandler('批量删除作业请求失败！'); });
          });
        }
      });
    },
    batchPriority() {
      this.batchOperation('优先Executors', (arr) => {
        const jobNames = this.getJobNameArray(arr);
        this.jobNamesArray = JSON.parse(JSON.stringify(jobNames));
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
          selectedJobNameArray.push(element);
        });
        callback(selectedJobNameArray);
      }
    },
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    handleCopy(row) {
      this.$http.get(`/console/namespaces/${this.domainName}/jobs/${row.jobName}/config`).then((data) => {
        const jobCopyInfo = {
          jobType: data.jobType,
          jobName: '',
          jobNameCopied: row.jobName,
          jobClass: data.jobClass,
          cron: data.cron,
          shardingTotalCount: data.shardingTotalCount,
          shardingItemParameters: data.shardingItemParameters,
          timeZone: data.timeZone,
          queueName: data.queueName,
          channelName: data.channelName,
          description: data.description,
        };
        this.isJobInfoVisible = true;
        this.jobInfoTitle = '复制作业';
        this.jobInfoOperation = 'copy';
        this.jobInfo = JSON.parse(JSON.stringify(jobCopyInfo));
      })
      .catch(() => { this.$http.buildErrorHandler('获取作业信息请求失败！'); });
    },
    handleDelete(row) {
      this.$message.confirmMessage(`确认删除作业 ${row.jobName} 吗?`, () => {
        this.$http.delete(`/console/namespaces/${this.domainName}/jobs/${row.jobName}`).then(() => {
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
        dependUrl = `/console/namespaces/${this.domainName}/jobs/dependency`;
        operation = '启用';
        text = '禁用';
        activeRequest = 'enable';
      } else {
        dependUrl = `/console/namespaces/${this.domainName}/jobs/beDependedJobs`;
        operation = '禁用';
        text = '启用';
        activeRequest = 'disable';
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
            this.batchActiveRequest(params, activeRequest);
          });
        } else {
          this.$message.confirmMessage(`确定${operation}作业${params.jobNames}吗?`, () => {
            this.batchActiveRequest(params, activeRequest);
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
        dependUrl = `/console/namespaces/${this.domainName}/jobs/${row.jobName}/dependency`;
        operation = '启用';
        text = '禁用';
        activeRequest = 'enable';
      } else {
        dependUrl = `/console/namespaces/${this.domainName}/jobs/${row.jobName}/beDependedJobs`;
        operation = '禁用';
        text = '启用';
        activeRequest = 'disable';
      }
      this.$http.get(dependUrl).then((data) => {
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
              this.activeRequest(row.jobName, activeRequest);
            });
          } else {
            this.$message.confirmMessage(`确定${operation}作业${row.jobName}吗?`, () => {
              this.activeRequest(row.jobName, activeRequest);
            });
          }
        } else {
          this.$message.confirmMessage(`确定${operation}作业${row.jobName}吗?`, () => {
            this.activeRequest(row.jobName, activeRequest);
          });
        }
      })
      .catch(() => { this.$http.buildErrorHandler(`${dependUrl}请求失败！`); });
    },
    batchActiveRequest(params, reqUrl) {
      this.$http.post(`/console/namespaces/${this.domainName}/jobs/${reqUrl}`, params).then(() => {
        this.$message.successNotify('操作成功');
        this.getJobList();
      })
      .catch(() => { this.$http.buildErrorHandler(`${reqUrl}请求失败！`); });
    },
    activeRequest(jobName, reqUrl) {
      this.$http.post(`/console/namespaces/${this.domainName}/jobs/${jobName}/${reqUrl}`, '').then(() => {
        this.$message.successNotify('操作成功');
        this.getJobList();
      })
      .catch(() => { this.$http.buildErrorHandler(`${reqUrl}请求失败！`); });
    },
    getJobNameArray(arr) {
      const jobNameArray = [];
      arr.forEach((ele) => {
        jobNameArray.push(ele.jobName);
      });
      return jobNameArray;
    },
    getUnstoptedJobArray(arr) {
      const resultArr = [];
      arr.forEach((ele) => {
        if (ele.status !== 'STOPPED') {
          resultArr.push(ele);
        }
      });
      return resultArr;
    },
    getStoptedJobArray(arr) {
      const resultArr = [];
      arr.forEach((ele) => {
        if (ele.status === 'STOPPED') {
          resultArr.push(ele);
        }
      });
      return resultArr;
    },
    getJobList() {
      return this.$http.get(`/console/namespaces/${this.domainName}/jobs`).then((data) => {
        this.jobList = data.jobs;
        this.totalNumber = data.totalNumber;
        this.enabledNumber = data.enabledNumber;
        this.abnormalNumber = data.abnormalNumber;
        this.total = data.length;
      })
      .catch(() => { this.$http.buildErrorHandler('获取作业列表失败！'); });
    },
    getGroupList() {
      return this.$http.get(`/console/namespaces/${this.domainName}/jobs/groups`).then((data) => {
        this.groupList = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取groups失败！'); });
    },
    init() {
      this.loading = true;
      Promise.all([this.getJobList(), this.getGroupList()]).then(() => {
        this.loading = false;
      });
    },
  },
  components: {
    'job-info-dialog': jobInfoDialog,
    'batch-priority-dialog': batchPriorityDialog,
    'import-result-dialog': importResultDialog,
  },
  created() {
    this.init();
  },
  watch: {
    $route: 'init',
  },
  computed: {
    domainName() {
      return this.$route.params.domain;
    },
    filterColumnPlaceholder() {
      let str = '';
      switch (this.selectColumn) {
        case 'jobName':
          str = '请输入作业名';
          break;
        case 'description':
          str = '请输入作业描述';
          break;
        default:
          str = '';
          break;
      }
      return str;
    },
  },
};
</script>
<style lang="sass" scoped>
.devicon-java-plain:before {
    content: "\F144";
}
</style>
