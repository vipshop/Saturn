<template>
    <div class="margin-20">
        <div v-if="abnormalJob === '' && timeoutJob === '' && unableFailoverJob === ''">
            <el-col :span="24">
                <div class="job-alarm-empty">
                    <span><i class="fa fa-info-circle"></i>当前没有告警信息</span>
                </div>
            </el-col>
        </div>
        <div v-else v-loading="loading" element-loading-text="请稍等···">
            <el-row :gutter="20">
                <el-col :span="8" v-if="abnormalJob !== ''">
                    <Panel type="warning" class="job-alarm-panel">
                        <div slot="title" class="job-alarm-title">异常告警</div>
                        <div slot="content">
                            <div class="job-alarm-content">
                                <div class="panel-row">{{abnormalJob.nextFireTimeWithTimeZoneFormat}}</div>
                                <div class="panel-row">{{$map.causeMap[abnormalJob.cause]}}</div>
                                <div class="panel-btn" v-if="$common.hasPerm('alarmCenter:setAbnormalJobRead', domainName)"><el-button :disabled="abnormalJob.read" size="small" type="warning" @click="handleAbnormalJob()">不再告警</el-button></div>
                            </div>
                        </div>
                    </Panel>
                </el-col>
                <el-col :span="8" v-if="timeoutJob !== ''">
                    <Panel type="warning" class="job-alarm-panel">
                        <div slot="title" class="job-alarm-title">超时告警</div>
                        <div slot="content">
                            <div class="job-alarm-content">
                                <div class="panel-row">超时秒数: {{timeoutJob.timeout4AlarmSeconds}}s</div>
                                <div class="panel-row">超时分片: {{timeoutJob.timeoutItems}}</div>
                                <div class="panel-btn" v-if="$common.hasPerm('alarmCenter:setTimeout4AlarmJobRead', domainName)"><el-button :disabled="timeoutJob.read" size="small" type="warning" @click="handleTimeoutJob()">不再告警</el-button></div>
                            </div>
                        </div>
                    </Panel>
                </el-col>
                <el-col :span="8" v-if="unableFailoverJob !== ''">
                    <Panel type="warning" class="job-alarm-panel">
                        <div slot="title" class="job-alarm-title">无法高可用告警</div>
                        <div slot="content">
                            <div class="job-alarm-content">
                                <div class="panel-row">域等级: {{$map.degreeMap[unableFailoverJob.degree]}}</div>
                                <div class="panel-row">作业等级: {{$map.degreeMap[unableFailoverJob.jobDegree]}}</div>
                            </div>
                        </div>
                    </Panel>
                </el-col>
            </el-row>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      loading: false,
      abnormalJob: '',
      timeoutJob: '',
      unableFailoverJob: '',
    };
  },
  methods: {
    getAbnormal() {
      return this.$http.get(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/isAbnormal`).then((data) => {
        this.abnormalJob = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取异常作业信息请求失败！'); });
    },
    getTimeout() {
      return this.$http.get(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/isTimeout4Alarm`).then((data) => {
        this.timeoutJob = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取超时作业信息请求失败！'); });
    },
    getUnableFailover() {
      return this.$http.get(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/isUnableFailover`).then((data) => {
        this.unableFailoverJob = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取无法高可用作业信息请求失败！'); });
    },
    handleAbnormalJob() {
      this.$http.post(`/console/namespaces/${this.domainName}/setAbnormalJobMonitorStatusToRead`, { uuid: this.abnormalJob.uuid }).then(() => {
        this.init();
        this.$message.successNotify('操作成功');
      })
      .catch(() => { this.$http.buildErrorHandler('不再告警操作请求失败！'); });
    },
    handleTimeoutJob() {
      this.$http.post(`/console/namespaces/${this.domainName}/setTimeout4AlarmJobMonitorStatusToRead`, { uuid: this.timeoutJob.uuid }).then(() => {
        this.init();
        this.$message.successNotify('操作成功');
      })
      .catch(() => { this.$http.buildErrorHandler('不再告警操作请求失败！'); });
    },
    init() {
      this.loading = true;
      Promise.all(
      [this.getAbnormal(), this.getTimeout(), this.getUnableFailover()]).then(() => {
        this.loading = false;
      });
    },
  },
  computed: {
    domainName() {
      return this.$route.params.domain;
    },
    jobName() {
      return this.$route.params.jobName;
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
.job-alarm-panel {
    height: 140px;
    animation:alarmBkg 2s infinite;
	-webkit-animation:alarmBkg 2s infinite;
    animation-timing-function:linear;
    -webkit-animation-timing-function:linear;
}
@keyframes alarmBkg
{
	from {box-shadow: 0px 0px 20px red;}
    50% {box-shadow: 0px 0px 20px #f80;}
	to {box-shadow: 0px 0px 20px red;}
}
@-webkit-keyframes alarmBkg /* Safari and Chrome */
{
	from {background: 0px 0px 20px red;}
	50% {box-shadow: 0px 0px 20px #f80;}
	to {box-shadow: 0px 0px 20px red;}
}
.job-alarm-title {
    font-size: 20px;
}
.job-alarm-content {
    font-size: 16px;
    margin: 10px 0;
    .panel-row {
        margin: 5px;
    }
    .panel-btn {
        margin: 15px 10px 0;
    }
}
.job-alarm-empty {
    text-align: center;
    span {
        font-size: 18px;
    }
}
</style>
