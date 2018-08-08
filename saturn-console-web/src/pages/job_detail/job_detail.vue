<template>
    <div>
        <Top-bar :domain="domainName" :domain-info="domainInfo"></Top-bar>
        <Aside :sidebar-menus="sidebarMenus" headerHeight="90">
            <div v-loading="loading" element-loading-text="请稍等···">
              <div class="job-detail-header">
                  <div class="pull-left job-detail-title">
                    <span class="job-span">
                        <i class="iconfont icon-java" v-if="jobInfo.jobType === 'JAVA_JOB'"></i>
                        <i class="iconfont icon-msnui-logo-linux" v-if="jobInfo.jobType === 'SHELL_JOB'"></i>
                        <i class="fa fa-envelope-o" v-if="jobInfo.jobType === 'MSG_JOB'"></i>
                        作业 : {{jobName}}
                    </span>
                    <el-tag :type="statusTag[jobInfo.status]" class="status-tag">运行状态: {{translateStatus[jobInfo.status]}}</el-tag>
                  </div>
                  <div class="pull-right">
                      <el-button size="small" @click="handleActive(true)" v-if="$common.hasPerm('job:enable', domainName) && (jobInfo.status === 'STOPPING' || jobInfo.status === 'STOPPED')"><i class="fa fa-play-circle text-btn"></i>启用</el-button>
                      <el-button size="small" @click="handleActive(false)" v-if="$common.hasPerm('job:disable', domainName) && (jobInfo.status === 'READY' || jobInfo.status === 'RUNNING')"><i class="fa fa-stop-circle text-warning"></i>禁用</el-button>
                      <el-button size="small" @click="handleOperate('runAtOnce')" v-if="$common.hasPerm('job:runAtOnce', domainName) && (jobInfo.status === 'READY' && jobStatusJudge() && jobInfo.jobType !== 'MSG_JOB')"><i class="fa fa-play-circle-o text-btn"></i>立即执行</el-button>
                      <el-button size="small" type="danger" @click="handleOperate('stopAtOnce')" v-if="$common.hasPerm('job:stopAtOnce', domainName) && jobInfo.status === 'STOPPING'"><i class="fa fa-stop-circle-o"></i>立即终止</el-button>
                      <el-button size="small" @click="handleDelete" v-if="$common.hasPerm('job:remove', domainName) && (jobInfo.status === 'STOPPED' || !jobInfo.enabled)"><i class="fa fa-trash text-danger"></i>删除</el-button>
                  </div>
              </div>
              <router-view></router-view>
            </div>
        </Aside>
    </div>
</template>

<script>
export default {
  data() {
    return {
      loading: false,
      domainInfo: {},
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
      jobShardings: [],
    };
  },
  methods: {
    handleOperate(operation) {
      if (operation === 'stopAtOnce') {
        const textHtml = `<span style="font-size:12px;color:#E6A23C;">* 注意：对于Java作业，立即终止操作（即强杀）会stop业务线程，如果作业代码没有实现postForceStop方法来释放资源，有可能导致资源的不释放，例如数据库连接的不释放。</span><br/>
        <span>确认立即终止作业 ${this.jobName} 吗?</span>`;
        this.$message.confirmMessage(textHtml, () => {
          this.operateRequest(operation);
        });
      } else {
        this.$message.confirmMessage(`确认立即执行作业 ${this.jobName} 吗?`, () => {
          this.operateRequest(operation);
        });
      }
    },
    operateRequest(operation) {
      this.$http.post(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/config/${operation}`, '').then(() => {
        this.$message.successNotify('操作成功');
        this.getJobInfo();
      })
      .catch(() => { this.$http.buildErrorHandler(`${operation}请求失败！`); });
    },
    handleDelete() {
      this.$message.confirmMessage(`确认删除作业 ${this.jobName} 吗?`, () => {
        this.$http.delete(`/console/namespaces/${this.domainName}/jobs/${this.jobName}`).then(() => {
          this.$router.push({ name: 'job_overview', params: { domain: this.domainName } });
          this.$message.successNotify('删除作业操作成功');
        })
        .catch(() => { this.$http.buildErrorHandler('删除作业请求失败！'); });
      });
    },
    handleActive(enabled) {
      let dependUrl = '';
      let operation = '';
      let text = '';
      let activeRequest = '';
      if (enabled) {
        dependUrl = `/console/namespaces/${this.domainName}/jobs/${this.jobName}/dependency`;
        operation = '启用';
        text = '禁用';
        activeRequest = 'enable';
      } else {
        dependUrl = `/console/namespaces/${this.domainName}/jobs/${this.jobName}/beDependedJobs`;
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
              this.activeRequest(this.jobName, activeRequest);
            });
          } else {
            this.$message.confirmMessage(`确定${operation}作业${this.jobName}吗?`, () => {
              this.activeRequest(this.jobName, activeRequest);
            });
          }
        } else {
          this.$message.confirmMessage(`确定${operation}作业${this.jobName}吗?`, () => {
            this.activeRequest(this.jobName, activeRequest);
          });
        }
      })
      .catch(() => { this.$http.buildErrorHandler(`${dependUrl}请求失败！`); });
    },
    activeRequest(jobName, reqUrl) {
      this.$http.post(`/console/namespaces/${this.domainName}/jobs/${jobName}/${reqUrl}`, '').then(() => {
        this.$message.successNotify('操作成功');
        this.getJobInfo();
      })
      .catch(() => { this.$http.buildErrorHandler(`${reqUrl}请求失败！`); });
    },
    getDomainInfo() {
      return this.$http.get(`/console/namespaces/${this.domainName}/`).then((data) => {
        this.domainInfo = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取namespace信息请求失败！'); });
    },
    getJobInfo() {
      const params = {
        domainName: this.domainName,
        jobName: this.jobName,
      };
      return this.$store.dispatch('setJobInfo', params).then((resp) => {
        console.log(resp);
      })
      .catch(() => this.$http.buildErrorHandler('获取作业信息请求失败！'));
    },
    jobStatusJudge() {
      return this.jobShardings.some((ele) => {
        if (ele.serverStatus === 'ONLINE') {
          return true;
        }
        return false;
      });
    },
    getJobStatus() {
      return this.$http.get(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/servers/status`).then((data) => {
        this.jobShardings = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取作业运行状态请求失败！'); });
    },
    init() {
      this.loading = true;
      Promise.all([this.getDomainInfo(), this.getJobInfo(), this.getJobStatus()]).then(() => {
        this.loading = false;
      });
    },
  },
  computed: {
    jobInfo() {
      return this.$store.state.global.jobInfo;
    },
    domainName() {
      return this.$route.params.domain;
    },
    jobName() {
      return this.$route.params.jobName;
    },
    sidebarMenus() {
      const menus = [
        { index: 'job_setting', title: '作业设置', icon: 'fa fa-gear', name: 'job_setting', params: { domain: this.$route.params.domain, jobName: this.$route.params.jobName } },
        { index: 'job_sharding', title: '分片情况', icon: 'fa fa-server', name: 'job_sharding', params: { domain: this.$route.params.domain, jobName: this.$route.params.jobName } },
        { index: 'job_execution', title: '运行状态', icon: 'fa fa-dot-circle-o', name: 'job_execution', params: { domain: this.$route.params.domain, jobName: this.$route.params.jobName } },
        { index: 'job_alarm', title: '告警中心', icon: 'fa fa-bell', name: 'job_alarm', params: { domain: this.$route.params.domain, jobName: this.$route.params.jobName } },
      ];
      return menus;
    },
  },
  watch: {
    $route: 'init',
  },
  created() {
    this.init();
  },
};
</script>
<style lang="sass" scoped>
.job-detail-header {
  padding: 10px;
  border-bottom: 1px solid #d0d0d0;
  height: 30px;
  .job-detail-title {
    height: 30px;
    line-height: 30px;
    .status-tag {
      font-size: 12px;
      margin-left: 5px;
    }
    .job-span {
      font-weight: bold;
      font-size: 16px;
      color: #777;
    }
  }
  >* {
    display: inline-block;
  }
}
</style>
