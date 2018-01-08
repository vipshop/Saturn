<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <Top-bar :domain="domainName" :domain-info="domainInfo"></Top-bar>
        <Aside :sidebar-menus="sidebarMenus" headerHeight="90">
            <div class="job-detail-header">
                <div class="pull-left job-detail-title">
                  <span class="job-span">作业 :  {{jobName}}</span>
                  <el-tag :type="statusTag[jobInfo.status]" class="status-tag">运行状态:{{jobInfo.status}}</el-tag>
                </div>
                <div class="pull-right">
                    <el-button size="small" @click="handleActive(true)" v-if="jobInfo.status === 'STOPPING' || jobInfo.status === 'STOPPED'"><i class="fa fa-play-circle text-btn"></i>启用</el-button>
                    <el-button size="small" @click="handleActive(false)" v-if="jobInfo.status === 'READY' || jobInfo.status === 'RUNNING'"><i class="fa fa-stop-circle text-btn"></i>禁用</el-button>
                    <el-button size="small" @click="" ><i class="fa fa-play-circle-o text-btn"></i>立即执行</el-button>
                    <el-button size="small" @click=""><i class="fa fa-stop-circle-o text-btn"></i>立即终止</el-button>
                    <el-button size="small" @click="handleDelete"><i class="fa fa-trash text-btn"></i>删除</el-button>
                </div>
            </div>
            <router-view></router-view>
        </Aside>
    </div>
</template>

<script>
export default {
  data() {
    return {
      loading: false,
      domainName: this.$route.params.domain,
      jobName: this.$route.params.jobName,
      sidebarMenus: [
        { index: 'job_setting', title: '作业设置', icon: 'fa fa-gear', name: 'job_setting', params: { domain: this.$route.params.domain, jobName: this.$route.params.jobName } },
        { index: 'job_sharding', title: '分片情况', icon: 'fa fa-server', name: 'job_sharding', params: { domain: this.$route.params.domain, jobName: this.$route.params.jobName } },
        { index: 'job_execution', title: '运行状态', icon: 'fa fa-dot-circle-o', name: 'job_execution', params: { domain: this.$route.params.domain, jobName: this.$route.params.jobName } },
        { index: 'job_statistics', title: '作业统计', icon: 'fa fa-bar-chart', name: 'job_statistics', params: { domain: this.$route.params.domain, jobName: this.$route.params.jobName } },
      ],
      domainInfo: {},
      jobInfo: {},
      statusTag: {
        READY: 'primary',
        RUNNING: 'success',
        STOPPING: 'warning',
        STOPPED: '',
      },
    };
  },
  methods: {
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
      this.loading = true;
      this.$http.get(`/console/namespaces/${this.domainName}`).then((data) => {
        this.domainInfo = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取namespace信息请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    getJobInfo() {
      this.$http.get(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/config`).then((data) => {
        this.jobInfo = JSON.parse(JSON.stringify(data));
      })
      .catch(() => { this.$http.buildErrorHandler('获取作业信息请求失败！'); });
    },
  },
  created() {
    this.getDomainInfo();
    this.getJobInfo();
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
