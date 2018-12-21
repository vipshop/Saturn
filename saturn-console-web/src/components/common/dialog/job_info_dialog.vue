<template>
    <div>
        <el-dialog :title="jobInfoTitle" :visible.sync="isVisible" :before-close="closeDialog" v-loading="loading" element-loading-text="请稍等···">
            <el-form :model="jobInfo" :rules="rules" ref="jobInfo" label-width="180px">
                <el-row>
                    <el-col :span="20">
                        <el-form-item label="作业类型" prop="jobType">
                            <el-select v-model="jobInfo.jobType" style="width: 100%" :disabled="!isEditable">
                                <el-option v-for="item in $option.jobTypes" :label="item.label" :value="item.value" :key="item.value"></el-option>
                            </el-select>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col :span="20">
                        <el-form-item label="作业名" prop="jobName">
                            <el-input v-model="jobInfo.jobName" placeholder="如SaturnJavaJob"></el-input>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col :span="20">
                        <el-form-item label="作业实现类" prop="jobClass" v-if="$option.isJava(jobInfo.jobType)">
                            <el-input v-model="jobInfo.jobClass" placeholder="如com.vip.saturn.job.SaturnJavaJob"></el-input>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col :span="20">
                        <el-form-item label="cron表达式" prop="cron" v-if="$option.isCron(jobInfo.jobType)">
                            <el-tooltip popper-class="form-tooltip" content="作业启动时间的cron表达式。如每10秒运行:*/10****?,每5分钟运行:0*/5***?" placement="bottom">
                                <el-input v-model="jobInfo.cron">
                                    <el-button slot="append" @click="checkAndForecastCron">预测</el-button>
                                </el-input>
                            </el-tooltip>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col>
                        <el-form-item class="form-annotation" v-if="$option.isCron(jobInfo.jobType)">
                            <span>1. 每10秒运行一次的表达式：*/10 * * * * ?</span><br/>
                            <span>2. 每分钟运行一次的表达式：0 * * * * ?</span>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col :span="20">
                        <el-form-item label="作业分片总数" prop="shardingTotalCount">
                            <el-tooltip popper-class="form-tooltip" placement="bottom" v-if="jobInfo.downStream">
                                <div slot="content">
                                      <span>{{ jobInfo.downStream.length > 0 ? '该作业有下游作业，作业分片数必须为1' : '作业分片数'}}</span>
                                </div>
                                <el-input-number v-model="jobInfo.shardingTotalCount" :disabled="jobInfo.downStream.length > 0" controls-position="right" :min="1" style="width: 100%;"></el-input-number>
                            </el-tooltip>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col :span="20">
                        <el-form-item label="分片参数" prop="shardingItemParameters">
                            <el-tooltip popper-class="form-tooltip" placement="bottom">
                                <div slot="content">
                                    分片序列号和参数用等号分隔，多个键值对用逗号分隔 。分片序列号从0开始，不可大于或等于作业分片总数。如：0=a,1=b,2=c;<br/>
                                    英文双引号请使用!!代替，英文等号请使用@@代替，英文逗号请使用##代替。<br/>
                                    如果作业所有分片无须参数，则只要保持值为0。例如有2个分片无须参数，则为“0=0,1=0”。<br/>
                                    对于本地模式的作业，格式为*=value
                                </div>
                                <el-input type="textarea" v-model="jobInfo.shardingItemParameters"></el-input>
                            </el-tooltip>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col :span="20">
                        <el-form-item label="自定义参数" prop="jobParameter">
                            <el-tooltip popper-class="form-tooltip" content="配置格式: 多个配置使用逗号分隔(key1=value1, key2=value2)。在分片序列号/参数对照表中可作为alias形式引用，格式为{key1}" placement="bottom">
                                <el-input type="textarea" v-model="jobInfo.jobParameter"></el-input>
                            </el-tooltip>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col :span="20">
                        <el-form-item prop="upStream" label="上游作业" v-if="$option.isPassive(jobInfo.jobType)">
                            <el-tooltip placement="bottom">
                                <div slot="content">
                                    满足上游作业的条件<br/>1.只能是定时作业或被动作业<br/>2.分片只能为1<br/>3.不能是本地模式作业<br/>4.不能是本作业的直接或者间接下游
                                </div>
                                <el-select filterable multiple v-model="jobInfo.upStream" style="width: 100%;">
                                    <el-option v-for="item in upStreamProvided" :label="item" :value="item" :key="item"></el-option>
                                </el-select>
                            </el-tooltip>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col :span="20">
                        <el-form-item prop="downStream" label="下游作业" v-if="$option.isPassive(jobInfo.jobType)">
                            <el-tooltip placement="bottom">
                                <div slot="content">
                                    配置下游作业的条件<br/>1.只能是定时作业或被动作业<br/>2.分片只能为1<br/>3.不能是本地模式作业<br/><br/>
                                    满足下游作业的条件<br/>1.只能是被动作业<br/>2.不能是本作业的直接或者间接上游
                                </div>
                                <el-select filterable multiple v-model="jobInfo.downStream" style="width: 100%;" :disabled="jobInfo.localMode || jobInfo.shardingTotalCount !== 1">
                                    <el-option v-for="item in downStreamProvided" :label="item" :value="item" :key="item"></el-option>
                                </el-select>
                            </el-tooltip>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row v-if="$option.isMsg(jobInfo.jobType)">
                    <el-col :span="20">
                        <el-form-item label="Queue名" prop="queueName">
                            <el-tooltip popper-class="form-tooltip" placement="bottom">
                                <div slot="content">
                                    消息类型job的queue名
                                </div>
                                <el-input type="textarea" v-model="jobInfo.queueName"></el-input>
                            </el-tooltip>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col :span="20">
                        <el-form-item label="描述" prop="description">
                            <el-input type="textarea" v-model="jobInfo.description"></el-input>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <div slot="footer" class="dialog-footer">    
                <el-button @click="closeDialog()">取消</el-button>
                <el-button type="primary" @click="handleSubmit()">确定</el-button>
            </div>
        </el-dialog>
        <div v-if="isCronPredictVisible">
            <CronPredictDialog :cron-predict-params="cronPredictParams" @close-dialog="closeCronDialog"></CronPredictDialog>
        </div>
    </div>
</template>

<script>
export default {
  props: ['domainName', 'jobInfo', 'jobInfoTitle', 'jobInfoOperation'],
  data() {
    return {
      isVisible: true,
      isCronPredictVisible: false,
      cronPredictParams: {},
      loading: false,
      rules: {
        jobType: [{ required: true, message: '请选择作业类型', trigger: 'change' }],
        jobName: [{ required: true, message: '作业名不能为空', trigger: 'blur' }],
        jobClass: [{ required: true, message: '作业实现类不能为空', trigger: 'blur' }],
        cron: [{ required: true, message: 'cron表达式不能为空', trigger: 'blur' }],
        shardingItemParameters: [{ required: true, message: '分片序列号/参数对照表不能为空', trigger: 'blur' }],
        queueName: [{ required: true, message: 'queue不能为空', trigger: 'blur' }],
      },
      upStreamProvided: [],
      downStreamProvided: [],
    };
  },
  methods: {
    checkAndForecastCron() {
      const cronData = {
        timeZone: this.jobInfo.timeZone,
        cron: this.jobInfo.cron,
      };
      this.cronPredictParams = JSON.parse(JSON.stringify(cronData));
      this.isCronPredictVisible = true;
    },
    closeCronDialog() {
      this.isCronPredictVisible = false;
    },
    handleSubmit() {
      this.$refs.jobInfo.validate((valid) => {
        if (valid) {
          if (this.validateShardingParamsNumber()) {
            if (this.validateShardingParam()) {
              if (this.jobInfoOperation === 'add') {
                this.jobInfoRequest(`/console/namespaces/${this.domainName}/jobs/jobs`);
              } else if (this.jobInfoOperation === 'copy') {
                this.jobInfoRequest(`/console/namespaces/${this.domainName}/jobs/${this.jobInfo.jobNameCopied}/copy`);
              }
            } else {
              this.$message.errorMessage('请正确输入分片参数!');
            }
          } else {
            this.$message.errorMessage('分片参数不能小于作业分片总数!');
          }
        }
      });
    },
    jobInfoRequest(url) {
      this.loading = true;
      this.$http.post(url, this.jobInfo).then(() => {
        this.$emit('job-info-success');
      })
      .catch(() => { this.$http.buildErrorHandler(`${url}请求失败！`); })
      .finally(() => {
        this.loading = false;
      });
    },
    closeDialog() {
      this.$emit('close-dialog');
    },
    validateShardingParamsNumber() {
      let flag = false;
      let arr = [];
      if (this.jobInfo.shardingItemParameters.indexOf(',') > 0) {
        arr = this.jobInfo.shardingItemParameters.split(',');
      } else {
        arr = [this.jobInfo.shardingItemParameters];
      }
      if (this.jobInfo.shardingTotalCount > arr.length) {
        flag = false;
      } else {
        flag = true;
      }
      return flag;
    },
    validateShardingParam() {
      let flag = false;
      let arr = [];
      if (this.jobInfo.shardingItemParameters.indexOf(',') > 0) {
        arr = this.jobInfo.shardingItemParameters.split(',');
      } else {
        arr = [this.jobInfo.shardingItemParameters];
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
    getUpStream() {
      return this.$http.get(`/console/namespaces/${this.domainName}/jobs/candidateUpStream`).then((data) => {
        this.upStreamProvided = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取上游作业失败！'); });
    },
    getDownStream() {
      return this.$http.get(`/console/namespaces/${this.domainName}/jobs/candidateDownStream`).then((data) => {
        this.downStreamProvided = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取下游作业失败！'); });
    },
    getJobStream() {
      this.loading = true;
      Promise.all([this.getUpStream(), this.getDownStream()]).then(() => {
        this.loading = false;
      });
    },
  },
  computed: {
    isEditable() {
      return this.jobInfoOperation !== 'copy';
    },
  },
  watch: {
    'jobInfo.shardingTotalCount': {
      immediate: true,
      handler(newCount) {
        if (newCount) {
          const result = [];
          if (Number(newCount) > 0 && (!this.jobInfo.shardingItemParameters || /^(\d*=)(,\d*=)*$/.test(this.jobInfo.shardingItemParameters))) {
            const arr = new Array(Number(newCount)).fill(1).map((ele, index) => index);
            arr.forEach((ele) => {
              result.push(`${ele}=`);
            });
            this.jobInfo.shardingItemParameters = result.join(',');
            console.info(this.jobInfo.shardingItemParameters);
          }
        }
      },
    },
    'jobInfo.jobType': {
      handler(newCount) {
        if (this.$option.isPassive(newCount)) {
          this.getJobStream();
        } else {
          this.jobInfo.upStream = [];
          this.jobInfo.downStream = [];
        }
      },
    },
  },
};
</script>
