<template>
    <div class="page-content" v-loading="loading" element-loading-text="请稍等···">
        <el-form :model="jobSettingInfo" :rules="rules" ref="jobSettingInfo" label-width="140px">
            <el-button type="primary" v-if="$common.hasPerm('job:update', domainName)" @click.stop="updateInfo" style="margin-bottom: 10px;" :disabled="jobSettingInfo.enabled"><i class="fa fa-database"></i>更新</el-button>
            <el-collapse v-model="activeNames">
                <el-collapse-item name="1">
                    <template slot="title">
                        基本配置
                    </template>
                    <div class="job-setting-content">
                        <el-row v-if="jobSettingInfo.jobType !== 'SHELL_JOB'">
                            <el-col :span="22">
                                <el-form-item prop="jobClass" label="作业实现类">
                                    <el-input v-model="jobSettingInfo.jobClass" disabled></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row v-if="jobSettingInfo.jobType !== 'MSG_JOB'">
                            <el-col :span="22">
                                <el-form-item prop="cron" label="Cron">
                                    <el-tooltip popper-class="form-tooltip" content="作业启动时间的cron表达式。如每10秒运行:*/10****?,每5分钟运行:0*/5***?" placement="bottom">
                                        <el-input v-model="jobSettingInfo.cron">
                                            <el-button slot="append" @click="checkAndForecastCron">预测</el-button>
                                        </el-input>
                                    </el-tooltip>
                                </el-form-item>
                                <el-form-item class="form-annotation">
                                    <span>1. 每10秒运行一次的表达式：*/10 * * * * ?</span><br/>
                                    <span>2. 每分钟运行一次的表达式：0 * * * * ?</span>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row :gutter="10">
                            <el-col :span="11">
                                <el-form-item prop="shardingTotalCount" label="作业分片数" v-if="!jobSettingInfo.localMode">
                                    <el-input-number v-model="jobSettingInfo.shardingTotalCount" controls-position="right" :min="1" style="width: 100%;"></el-input-number>
                                </el-form-item>
                                <el-form-item prop="shardingTotalCount" label="作业分片数" v-if="jobSettingInfo.localMode">
                                    <el-input value="N/A" disabled style="width: 100%;"></el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11">
                                <el-form-item prop="localMode" label="本地模式">
                                    <el-switch v-model="jobSettingInfo.localMode" @change="localModeChange"></el-switch>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row v-if="jobSettingInfo.jobType === 'MSG_JOB'">
                            <el-col :span="22">
                                <el-form-item prop="queueName" label="Queue名">
                                    <el-tooltip popper-class="form-tooltip" placement="bottom">
                                        <div slot="content">
                                            消息类型job的queue名，2.0.0版本的executor支持为每个分片配置一个queue，格式为0=queue1,1=queue2
                                        </div>
                                        <el-input type="textarea" v-model="jobSettingInfo.queueName"></el-input>
                                    </el-tooltip>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="shardingItemParameters" label="分片参数">
                                    <el-tooltip popper-class="form-tooltip" placement="bottom">
                                        <div slot="content">
                                            分片序列号和参数用等号分隔，多个键值对用逗号分隔 。分片序列号从0开始，不可大于或等于作业分片总数。如：0=a,1=b,2=c;<br/>
                                            英文双引号请使用!!代替，英文等号请使用@@代替，英文逗号请使用##代替。<br/>
                                            如果作业所有分片无须参数，则只要保持值为0。例如有2个分片无须参数，则为“0=0”。<br/>
                                            对于本地模式的作业，格式为*=value
                                        </div>
                                        <el-input type="textarea" v-model="jobSettingInfo.shardingItemParameters"></el-input>
                                    </el-tooltip>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="jobParameter" label="自定义参数">
                                    <el-tooltip popper-class="form-tooltip" content="配置格式: 多个配置使用逗号分隔(key1=value1, key2=value2)。在分片序列号/参数对照表中可作为alias形式引用，格式为{key1}" placement="bottom">
                                        <el-input type="textarea" v-model="jobSettingInfo.jobParameter"></el-input>
                                    </el-tooltip>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="preferList" label="优先executor">
                                    <el-select size="small" filterable multiple v-model="jobSettingInfo.preferList" style="width: 100%;">
                                        <el-option v-for="item in preferListProvidedArray" :label="item.executorDes" :value="item.executorName" :key="item.executorDes">
                                        </el-option>
                                    </el-select>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="6" v-if="!jobSettingInfo.localMode">
                                <el-form-item prop="onlyUsePreferList" label="只使用优先executor">
                                    <el-switch v-model="jobSettingInfo.onlyUsePreferList"></el-switch>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="description" label="作业描述信息">
                                    <el-input type="textarea" v-model="jobSettingInfo.description"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                    </div>
                </el-collapse-item>
                <el-collapse-item name="2">
                    <template slot="title">
                        高级配置
                    </template>
                    <div class="job-setting-content">
                        <el-row>
                            <el-col :span="11">
                                <el-form-item prop="timeout4AlarmSeconds" label="超时告警(秒)">
                                    <el-input v-model="jobSettingInfo.timeout4AlarmSeconds"></el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11">
                                <el-form-item prop="timeoutSeconds" label="超时强杀(秒)">
                                    <el-tooltip popper-class="form-tooltip" placement="top" effect="light">
                                        <div slot="content">
                                            <span class="text-warning">警告：对于Java作业，立即终止操作（即强杀）会stop业务线程，如果作业代码没有实现onTimeout方法来释放资源，有可能导致资源的不释放，例如数据库连接的不释放。</span>
                                        </div>
                                        <el-input v-model="jobSettingInfo.timeoutSeconds"></el-input>
                                    </el-tooltip>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="groups" label="所属分组">
                                    <el-input v-model="jobSettingInfo.groups"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="11">
                                <el-form-item prop="loadLevel" label="作业负荷">
                                    <el-input v-model="jobSettingInfo.loadLevel"></el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11">
                                <el-form-item prop="processCountIntervalSeconds" label="统计处理间隔(秒)">
                                    <el-input v-model="jobSettingInfo.processCountIntervalSeconds"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="timeZone" label="时区">
                                    <el-select filterable v-model="jobSettingInfo.timeZone" style="width: 100%;">
                                        <el-option v-for="item in jobSettingInfo.timeZonesProvided" :label="item" :value="item" :key="item"></el-option>
                                    </el-select>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row :gutter="10">
                            <el-col :span="jobSettingInfo.jobType === 'MSG_JOB' ? 7 : 11">
                                <el-form-item prop="showNormalLog" label="控制台输出日志">
                                    <el-switch v-model="jobSettingInfo.showNormalLog"></el-switch>
                                </el-form-item>
                            </el-col>
                            <el-col :span="jobSettingInfo.jobType === 'MSG_JOB' ? 7 : 11">
                                <el-form-item prop="enabledReport" label="上报运行状态">
                                    <el-switch v-model="jobSettingInfo.enabledReport" @change="enabledReportChange"></el-switch>
                                </el-form-item>
                            </el-col>
                            <el-col :span="jobSettingInfo.jobType === 'MSG_JOB' ? 7 : 11" v-if="jobSettingInfo.jobType === 'MSG_JOB'">
                                <el-form-item prop="useSerial" label="串行消费">
                                    <el-switch v-model="jobSettingInfo.useSerial"></el-switch>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row :gutter="10" v-if="jobSettingInfo.jobType !== 'MSG_JOB'">
                            <el-col :span="11">
                                <el-form-item prop="failover" label="故障转移">
                                    <el-switch v-model="jobSettingInfo.failover" title="本地模式或非上报运行状态不可编辑" :disabled="jobSettingInfo.localMode || !jobSettingInfo.enabledReport"></el-switch>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11">
                                <el-form-item prop="rerun" label="过时未跑重试">
                                    <el-switch v-model="jobSettingInfo.rerun" title="非上报运行状态不可编辑" :disabled="!jobSettingInfo.enabledReport"></el-switch>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="dependencies" label="依赖作业">
                                    <el-select size="small" filterable multiple v-model="jobSettingInfo.dependencies" style="width: 100%;">
                                        <el-option v-for="item in jobSettingInfo.dependenciesProvided" :label="item" :value="item" :key="item"> </el-option>
                                    </el-select>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="channelName" label="执行结果发送的Channel名">
                                    <el-tooltip popper-class="form-tooltip" content="执行消息作业结果发送的channel名，注意不能跟queue绑定的channel一致，以免造成死循环" placement="bottom">
                                        <el-input v-model="jobSettingInfo.channelName"></el-input>
                                    </el-tooltip>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row v-if="jobSettingInfo.jobType !== 'MSG_JOB'">
                            <el-col :span="22">
                                <el-form-item prop="pausePeriodDate" label="暂停日期段">
                                    <el-tooltip popper-class="form-tooltip" content="日期时间段，支持多个日期段。例如03/12-03/15。当日期为空，时间段不为空，表示每天那些时间段都暂停" placement="bottom">
                                        <InputTags :dynamic-tags="jobSettingInfo.pausePeriodDate" title="日期段"></InputTags>
                                    </el-tooltip>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row v-if="jobSettingInfo.jobType !== 'MSG_JOB'">
                            <el-col :span="22">
                                <el-form-item prop="pausePeriodTime" label="暂停时间段">
                                    <el-tooltip popper-class="form-tooltip" content="日期时间段，支持多个时间段。例如12:23-13:23。当日期为不空，时间段为空，表示那些日期段24小时都暂停。针对跨日的时间段，
                                    编辑时需要分成2个段，以23:00-03:00为例，需写成23:00-23:59和00:00-03:00" placement="bottom">
                                        <InputTags :dynamic-tags="jobSettingInfo.pausePeriodTime" title="时间段"></InputTags>
                                    </el-tooltip>
                                </el-form-item>
                            </el-col>
                        </el-row>
                    </div>
                </el-collapse-item>
            </el-collapse>
            <el-button type="primary" v-if="$common.hasPerm('job:update', domainName)" @click.stop="updateInfo" style="margin-top: 10px;" :disabled="jobSettingInfo.enabled"><i class="fa fa-database"></i>更新</el-button>
        </el-form>
        <div v-if="isCronPredictVisible">
            <CronPredictDialog :cron-predict-params="cronPredictParams" @close-dialog="closeCronDialog"></CronPredictDialog>
        </div>
    </div>
</template>
<script>
export default {
  props: [],
  data() {
    return {
      loading: false,
      isCronPredictVisible: false,
      activeNames: ['1'],
      cronPredictParams: {},
      rules: {
        cron: [{ required: true, message: 'cron表达式不能为空', trigger: 'blur' }],
        shardingItemParameters: [{ required: true, message: '分片序列号/参数对照表不能为空', trigger: 'blur' }],
      },
      preferListProvidedArray: [],
    };
  },
  methods: {
    localModeChange(value) {
      if (!value) {
        if (this.jobSettingInfo.enabledReport) {
          this.jobSettingInfo.failover = true;
        } else {
          this.jobSettingInfo.failover = false;
        }
      } else {
        this.jobSettingInfo.failover = false;
      }
    },
    enabledReportChange(value) {
      if (value) {
        if (this.jobSettingInfo.localMode) {
          this.jobSettingInfo.failover = false;
        } else {
          this.jobSettingInfo.failover = true;
        }
      } else {
        this.jobSettingInfo.failover = false;
        this.jobSettingInfo.rerun = false;
      }
    },
    checkAndForecastCron() {
      const cronData = {
        timeZone: this.jobSettingInfo.timeZone,
        cron: this.jobSettingInfo.cron,
      };
      this.cronPredictParams = JSON.parse(JSON.stringify(cronData));
      this.isCronPredictVisible = true;
    },
    closeCronDialog() {
      this.isCronPredictVisible = false;
    },
    updateInfo() {
      this.$refs.jobSettingInfo.validate((valid) => {
        if (valid) {
          if (this.validateLocalMode()) {
            if (this.jobSettingInfo.localMode) {
              this.jobSettingInfo.shardingTotalCount = 1;
            }
            if (this.validateShardingParamsNumber()) {
              if (this.validateShardingParam()) {
                this.jobSettingInfoRequest();
              } else {
                this.$message.errorMessage('请正确输入分片参数!');
              }
            } else {
              this.$message.errorMessage('分片参数不能小于作业分片总数!');
            }
          } else {
            this.$message.errorMessage('作业分片参数有误，对于本地模式的作业，只需要输入如：*=a 即可。');
          }
        }
      });
    },
    jobSettingInfoRequest() {
      if (this.jobSettingInfo.preferList.length > 0) {
        this.jobSettingInfo.preferList.forEach((ele1, index1) => {
          this.jobSettingInfo.preferListProvided.forEach((ele2) => {
            if (ele1 === ele2.executorName && ele2.type === 'DOCKER') {
              this.jobSettingInfo.preferList[index1] = `@${ele1}`;
            }
          });
        });
      }
      this.$http.post(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/config`, this.jobSettingInfo).then(() => {
        this.getJobSettingInfo();
        this.$message.successNotify('更新作业操作成功');
      })
      .catch(() => { this.$http.buildErrorHandler('更新作业请求失败！'); });
    },
    validateLocalMode() {
      let flag = true;
      if (this.jobSettingInfo.localMode) {
        if (!this.jobSettingInfo.shardingItemParameters.startsWith('*=')) {
          flag = false;
        }
      }
      return flag;
    },
    validateShardingParamsNumber() {
      let flag = false;
      if (!this.jobSettingInfo.localMode) {
        let arr = [];
        if (this.jobSettingInfo.shardingItemParameters.indexOf(',') > 0) {
          arr = this.jobSettingInfo.shardingItemParameters.split(',');
        } else {
          arr = [this.jobSettingInfo.shardingItemParameters];
        }
        if (this.jobSettingInfo.shardingTotalCount > arr.length) {
          flag = false;
        } else {
          flag = true;
        }
      } else {
        flag = true;
      }
      return flag;
    },
    validateShardingParam() {
      let flag = false;
      let arr = [];
      if (this.jobSettingInfo.shardingItemParameters.indexOf(',') > 0) {
        arr = this.jobSettingInfo.shardingItemParameters.split(',');
      } else {
        arr = [this.jobSettingInfo.shardingItemParameters];
      }
      arr.forEach((ele) => {
        if (ele.indexOf('=') > -1) {
          if (ele.split('=')[0] === '') {
            flag = false;
          } else {
            flag = true;
          }
        } else {
          flag = false;
        }
      });
      return flag;
    },
    getJobSettingInfo() {
      const params = {
        domainName: this.domainName,
        jobName: this.jobName,
      };
      this.loading = true;
      this.$store.dispatch('setJobInfo', params).then((resp) => {
        console.log(resp);
      })
      .catch(() => this.$http.buildErrorHandler('获取作业信息请求失败！'))
      .finally(() => {
        this.loading = false;
      });
    },
  },
  computed: {
    jobSettingInfo() {
      const jobInfoData = this.$store.state.global.jobInfo;
      if (jobInfoData.preferList) {
        jobInfoData.preferList.forEach((ele, index) => {
          if (ele.startsWith('@')) {
            jobInfoData.preferList[index] = ele.replace('@', '');
          }
        });
      }
      if (jobInfoData.preferListProvided) {
        this.preferListProvidedArray = jobInfoData.preferListProvided.map((obj) => {
          const rObj = {};
          rObj.ip = obj.ip;
          rObj.executorName = obj.executorName;
          rObj.noTraffic = obj.noTraffic;
          rObj.type = obj.type;
          rObj.status = obj.status;
          if (rObj.type === 'PHYSICAL') {
            if (rObj.noTraffic) {
              switch (rObj.status) {
                case 'OFFLINE':
                  rObj.executorDes = `${rObj.executorName}(离线无流量)`;
                  break;
                case 'ONLINE':
                  rObj.executorDes = `${rObj.executorName}(在线无流量 ${rObj.ip})`;
                  break;
                case 'DELETED':
                  rObj.executorDes = `${rObj.executorName}(已删除)`;
                  break;
                default:
                  break;
              }
            } else {
              switch (rObj.status) {
                case 'OFFLINE':
                  rObj.executorDes = `${rObj.executorName}(离线)`;
                  break;
                case 'ONLINE':
                  rObj.executorDes = `${rObj.executorName}(在线 ${rObj.ip})`;
                  break;
                case 'DELETED':
                  rObj.executorDes = `${rObj.executorName}(已删除)`;
                  break;
                default:
                  break;
              }
            }
          } else {
            switch (rObj.status) {
              case 'DELETED':
                rObj.executorDes = `${rObj.executorName}(容器组已删除)`;
                break;
              default:
                rObj.executorDes = `${rObj.executorName}(容器组)`;
                break;
            }
          }
          return rObj;
        });
      }
      return jobInfoData;
    },
    domainName() {
      return this.$route.params.domain;
    },
    jobName() {
      return this.$route.params.jobName;
    },
  },
  watch: {
    $route: 'getJobSettingInfo',
  },
};
</script>
<style lang="sass">
.job-setting-content {
    padding: 10px 15% 0;
}
</style>
