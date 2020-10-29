<template>
    <div class="page-content" v-loading="loading" element-loading-text="请稍等···">
        <el-form :model="jobSettingInfo" :rules="rules" ref="jobSettingInfo" label-width="140px">
            <div v-if="$common.hasPerm('job:update', domainName)" style="margin-bottom: 10px;">
              <el-button type="primary" @click.stop="updateInfo(false)" :disabled="jobSettingInfo.enabled"><i class="fa fa-database"></i>更新</el-button>
              <el-button type="primary" @click.stop="updateInfo(true)" :disabled="jobSettingInfo.enabled"><i class="fa fa-play-circle"></i>更新并启用</el-button>
            </div>
            <el-collapse v-model="activeNames">
                <el-collapse-item name="1">
                    <template slot="title">
                        基本配置
                    </template>
                    <div class="job-setting-content">
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="jobType" label="作业类型">
                                    <el-input v-model="jobSettingInfo.jobType" disabled></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row v-if="$option.isJava(jobSettingInfo.jobType)">
                            <el-col :span="22">
                                <el-form-item prop="jobClass" label="作业实现类">
                                    <el-input v-model="jobSettingInfo.jobClass" disabled></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row v-if="$option.isCron(jobSettingInfo.jobType)">
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
                                    <el-tooltip popper-class="form-tooltip" placement="bottom" v-if="jobSettingInfo.downStream">
                                        <div slot="content">
                                             <span>{{ jobSettingInfo.downStream.length > 0 ? '该作业有下游作业，作业分片数必须为1' : '作业分片数'}}</span>
                                        </div>
                                        <el-input-number v-model="jobSettingInfo.shardingTotalCount" :disabled="jobSettingInfo.downStream.length > 0" controls-position="right" :min="1" style="width: 100%;"></el-input-number>
                                    </el-tooltip>
                                </el-form-item>
                                <el-form-item prop="shardingTotalCount" label="作业分片数" v-if="jobSettingInfo.localMode">
                                    <el-input value="N/A" disabled style="width: 100%;"></el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11">
                                <el-form-item prop="localMode" label="本地模式">
                                    <el-tooltip popper-class="form-tooltip" placement="bottom" v-if="jobSettingInfo.downStream">
                                        <div slot="content">
                                             <span>{{ jobSettingInfo.downStream.length > 0 ? '该作业有下游作业，不可选为本地模式' : '本地模式'}}</span>
                                        </div>
                                        <el-switch v-model="jobSettingInfo.localMode" :disabled="jobSettingInfo.downStream.length > 0"></el-switch>
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
                                            如果作业所有分片无须参数，则只要保持值为0。例如有2个分片无须参数，则为“0=0,1=0”。<br/>
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
                                    <el-input type="textarea" v-model="jobSettingInfo.jobParameter"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="preferList" label="优先executor">
                                    <el-select filterable multiple v-model="jobSettingInfo.preferList" style="width: 100%;">
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
                                <el-form-item prop="timeout4AlarmSeconds" label="运行超时告警(秒)">
                                    <el-input v-model="jobSettingInfo.timeout4AlarmSeconds"></el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11">
                                <el-form-item prop="timeoutSeconds" label="运行超时强杀(秒)">
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
                            <el-col :span="11">
                                <el-form-item prop="disableTimeoutSeconds" label="禁用超时告警(秒)">
                                    <el-tooltip popper-class="form-tooltip" placement="top" effect="light">
                                        <div slot="content">
                                            <span class="text-warning">当禁用作业时长超过该值则触发告警，0为不告警</span>
                                        </div>
                                        <el-input v-model="jobSettingInfo.disableTimeoutSeconds"></el-input>
                                    </el-tooltip>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="groups" label="所属分组">
                                    <el-tag
                                      :key="group"
                                      v-for="group in jobSettingInfo.groups"
                                      type="primary"
                                      closable
                                      :disable-transitions="false"
                                      style="margin: 0 3px 3px 0;"
                                      @close="handleDeleteGroup(group)">
                                      {{group}}
                                    </el-tag>
                                    <el-autocomplete
                                      v-if="inputGroupVisible"
                                      v-model="groupSelected"
                                      ref="saveGroupInput"
                                      :fetch-suggestions="querySearchGroups"
                                      placeholder="请添加或选择分组"
                                      @select="handleInputGroup"
                                      @keyup.enter.native="handleInputGroup"
                                    >
                                    </el-autocomplete>
                                    <el-button v-else size="small" @click="showInput">+ 分组</el-button>
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
                            <el-col :span="$option.isMsg(jobSettingInfo.jobType) ? 7 : 11">
                                <el-form-item prop="showNormalLog" label="控制台输出日志">
                                    <el-switch v-model="jobSettingInfo.showNormalLog"></el-switch>
                                </el-form-item>
                            </el-col>
                            <el-col :span="$option.isMsg(jobSettingInfo.jobType) ? 7 : 11">
                                <el-form-item prop="enabledReport" label="上报运行状态">
                                    <el-switch v-model="jobSettingInfo.enabledReport"></el-switch>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row :gutter="10" v-if="$option.isCron(jobSettingInfo.jobType) || $option.isPassive(jobSettingInfo.jobType)">
                            <el-col :span="$option.isCron(jobSettingInfo.jobType) ? 11: 22">
                                <el-form-item prop="failover" label="故障转移">
                                    <el-switch v-model="jobSettingInfo.failover" title="本地模式或非上报运行状态不可编辑" :disabled="jobSettingInfo.localMode || !jobSettingInfo.enabledReport"></el-switch>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11" v-if="$option.isCron(jobSettingInfo.jobType)">
                                <el-form-item prop="rerun">
                                    <div slot="label">
                                        <span>过时未跑重试</span>
                                        <el-tooltip placement="top" content="不建议高频作业启动超时重跑" effect="light" popper-class="allocation-popper">
                                            <i class="fa fa-question-circle"></i>
                                        </el-tooltip>
                                    </div>
                                    <el-switch v-model="jobSettingInfo.rerun" title="非上报运行状态不可编辑" :disabled="!jobSettingInfo.enabledReport"></el-switch>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row v-if="$option.isPassive(jobSettingInfo.jobType)">
                            <el-col :span="22">
                                <el-form-item prop="upStream" label="上游作业">
                                    <el-tooltip placement="bottom">
                                        <div slot="content">
                                            满足上游作业的条件<br/>1.只能是定时作业或被动作业<br/>2.分片只能为1<br/>3.不能是本地模式作业<br/>4.不能是本作业的直接或者间接下游
                                        </div>
                                        <el-select filterable multiple v-model="jobSettingInfo.upStream" style="width: 100%;">
                                            <el-option v-for="item in jobSettingInfo.upStreamProvided" :label="item" :value="item" :key="item"></el-option>
                                        </el-select>
                                    </el-tooltip>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row v-if="$option.isCron(jobSettingInfo.jobType) || $option.isPassive(jobSettingInfo.jobType)">
                            <el-col :span="22">
                                <el-form-item prop="downStream" label="下游作业">
                                    <div slot="label">
                                        下游作业
                                        <el-tooltip content="查看作业依赖图" placement="top">
                                            <el-button type="text" @click="handleArrangeLayout()"><i class="fa fa-search-plus"></i></el-button>
                                        </el-tooltip>
                                    </div>
                                    <el-tooltip placement="bottom">
                                        <div slot="content">
                                            配置下游作业的条件<br/>1.只能是定时作业或被动作业<br/>2.分片只能为1<br/>3.不能是本地模式作业<br/><br/>
                                            满足下游作业的条件<br/>1.只能是被动作业<br/>2.不能是本作业的直接或者间接上游
                                        </div>
                                        <el-select filterable multiple v-model="jobSettingInfo.downStream" style="width: 100%;" :disabled="jobSettingInfo.localMode || jobSettingInfo.shardingTotalCount !== 1">
                                            <el-option v-for="item in jobSettingInfo.downStreamProvided" :label="item" :value="item" :key="item"></el-option>
                                        </el-select>
                                    </el-tooltip>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row v-if="$option.isCron(jobSettingInfo.jobType)">
                            <el-col :span="22">
                                <el-form-item prop="pausePeriodDate" label="暂停日期段">
                                    <el-tooltip popper-class="form-tooltip" content="日期时间段，支持多个日期段。例如03/12-03/15。当日期为空，时间段不为空，表示每天那些时间段都暂停" placement="bottom">
                                        <InputTags :dynamic-tags="jobSettingInfo.pausePeriodDate" title="日期段"></InputTags>
                                    </el-tooltip>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row v-if="$option.isCron(jobSettingInfo.jobType)">
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
            <div v-if="$common.hasPerm('job:update', domainName)" style="margin-top: 10px;">
              <el-button type="primary" @click.stop="updateInfo(false)" :disabled="jobSettingInfo.enabled"><i class="fa fa-database"></i>更新</el-button>
              <el-button type="primary" @click.stop="updateInfo(true)" :disabled="jobSettingInfo.enabled"><i class="fa fa-play-circle"></i>更新并启用</el-button>
            </div>
        </el-form>
        <div v-if="isCronPredictVisible">
            <CronPredictDialog :cron-predict-params="cronPredictParams" @close-dialog="closeCronDialog"></CronPredictDialog>
        </div>
        <div v-if="isArrangeLayoutVisible">
            <arrange-layout-dialog :arrange-layout-info="arrangeLayoutInfo" @job-redirect="jobRedirect" @close-dialog="closeArrangeLayoutDialog"></arrange-layout-dialog>
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
        queueName: [{ required: true, message: 'queue不能为空', trigger: 'blur' }],
      },
      preferListProvidedArray: [],
      isArrangeLayoutVisible: false,
      arrangeLayoutInfo: {},
      inputGroupVisible: false,
      groupSelected: '',
      groupList: [],
    };
  },
  methods: {
    handleDeleteGroup(group) {
      this.jobSettingInfo.groups.splice(this.jobSettingInfo.groups.indexOf(group), 1);
    },
    showInput() {
      this.inputGroupVisible = true;
      this.$nextTick(() => {
        this.$refs.saveGroupInput.$refs.input.focus();
      });
    },
    validateGroup(group) {
      let flag = false;
      const parten = /^[\u4e00-\u9fa5a-zA-Z0-9_.-]+$/;
      if (group.length > 15) {
        this.$message.errorMessage('分组名称不能超过15个字符');
      } else if (!parten.test(group)) {
        this.$message.errorMessage('分组名称不能使用特殊字符');
      } else if (this.jobSettingInfo.groups.includes(group)) {
        this.$message.errorMessage('该分组已被选择！');
      } else {
        flag = true;
      }
      return flag;
    },
    handleInputGroup() {
      const groupSelected = this.groupSelected;
      if (groupSelected) {
        if (this.validateGroup(groupSelected)) {
          this.jobSettingInfo.groups.push(groupSelected);
        }
      }
      this.inputGroupVisible = false;
      this.groupSelected = '';
    },
    querySearchGroups(queryString, cb) {
      const groupList = this.groupList;
      const results = queryString ?
      groupList.filter(this.createStateFilter(queryString)) : groupList;
      cb(results);
    },
    createStateFilter(queryString) {
      return state =>
        state.value.indexOf(queryString) >= 0;
    },
    handleArrangeLayout() {
      this.$http.get(`/console/namespaces/${this.domainName}/jobs/arrangeLayout`).then((data) => {
        this.arrangeLayoutInfo = data;
        if (this.arrangeLayoutInfo.paths.length > 0) {
          this.isArrangeLayoutVisible = true;
        } else {
          this.$message.errorMessage('很抱歉！该作业没有依赖');
        }
      })
      .catch(() => { this.$http.buildErrorHandler('请求失败！'); });
    },
    jobRedirect(jobName) {
      const routeData = this.$router.resolve({ name: 'job_setting', params: { domain: this.domainName, jobName } });
      window.open(routeData.href, '_blank');
    },
    closeArrangeLayoutDialog() {
      this.isArrangeLayoutVisible = false;
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
    updateInfo(active) {
      this.$refs.jobSettingInfo.validate((valid) => {
        if (valid) {
          const paramsInfo = JSON.parse(JSON.stringify(this.jobSettingInfo));
          if (paramsInfo.groups.length > 0) {
            this.$set(paramsInfo, 'groups', paramsInfo.groups.join(','));
          }
          if (this.validateLocalMode()) {
            if (paramsInfo.localMode) {
              paramsInfo.shardingTotalCount = 1;
            }
            if (paramsInfo.preferList.length > 0) {
              paramsInfo.preferList.forEach((ele1, index1) => {
                paramsInfo.preferListProvided.forEach((ele2) => {
                  if (ele1 === ele2.executorName && ele2.type === 'DOCKER') {
                    // eslint-disable-next-line no-param-reassign
                    paramsInfo.preferList[index1] = `@${ele1}`;
                  }
                });
              });
            }
            if (this.validateShardingParamsNumber()) {
              if (this.validateShardingParam()) {
                this.jobSettingInfoRequest(paramsInfo, active);
              } else {
                this.$message.errorMessage('请正确输入分片参数!');
              }
            } else {
              this.$message.errorMessage('分片参数不能小于作业分片总数!');
            }
          } else {
            this.$message.errorMessage('作业分片参数有误，对于本地模式的作业，只需要输入如：*=a 即可。非本地模式请参考:0=a,1=b');
          }
        }
      });
    },
    jobSettingInfoRequest(paramsInfo, active) {
      this.loading = true;
      this.$http.post(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/config`, paramsInfo).then(() => {
        if (active) {
          this.activeRequest();
        } else {
          this.updateInfoSuccess();
          this.$message.successNotify('更新作业操作成功');
        }
      })
      .catch(() => { this.$http.buildErrorHandler('更新作业请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    activeRequest() {
      this.loading = true;
      this.$http.post(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/enable`, '').then(() => {
        this.updateInfoSuccess();
        this.$message.successNotify('更新并启用作业操作成功');
      })
      .catch(() => { this.$http.buildErrorHandler('更新并启用作业请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    updateInfoSuccess() {
      this.loading = true;
      Promise.all([this.getGroupList(), this.getJobSettingInfo()]).then()
      .finally(() => {
        this.loading = false;
      });
    },
    validateLocalMode() {
      let flag = true;
      if (this.jobSettingInfo.localMode) {
        if (!this.jobSettingInfo.shardingItemParameters.startsWith('*=')) {
          flag = false;
        }
      } else if (this.jobSettingInfo.shardingItemParameters.startsWith('*=')) {
        flag = false;
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
      this.$store.dispatch('setJobInfo', params).then((resp) => {
        console.log(resp);
      })
      .catch(() => this.$http.buildErrorHandler('获取作业信息请求失败！'));
    },
    getGroupList() {
      return this.$http.get(`/console/namespaces/${this.domainName}/jobs/groups`).then((data) => {
        this.groupList = data.filter(v => v !== '未分组').map((obj) => {
          const rObj = {};
          rObj.value = obj;
          return rObj;
        });
      })
      .catch(() => { this.$http.buildErrorHandler('获取groups失败！'); });
    },
    init() {
      this.loading = true;
      Promise.all([this.getGroupList()]).then(() => {
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
    'jobSettingInfo.shardingTotalCount': {
      immediate: true,
      handler(newCount) {
        if (newCount) {
          const result = [];
          if (Number(newCount) > 0 && (!this.jobSettingInfo.shardingItemParameters || /^(\d*=)(,\d*=)*$/.test(this.jobSettingInfo.shardingItemParameters))) {
            const arr = new Array(Number(newCount)).fill(1).map((ele, index) => index);
            arr.forEach((ele) => {
              result.push(`${ele}=`);
            });
            this.jobSettingInfo.shardingItemParameters = result.join(',');
          }
        }
      },
    },
    'jobSettingInfo.localMode': {
      immediate: true,
      handler(newCount) {
        if (!newCount) {
          if (this.jobSettingInfo.enabledReport) {
            this.jobSettingInfo.failover = true;
          } else {
            this.jobSettingInfo.failover = false;
          }
        } else {
          this.jobSettingInfo.failover = false;
        }
      },
    },
    'jobSettingInfo.enabledReport': {
      immediate: true,
      handler(newCount) {
        if (newCount) {
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
    },
  },
  created() {
    this.init();
  },
};
</script>
<style lang="sass">
.job-setting-content {
    padding: 10px 15% 0;
}
</style>
