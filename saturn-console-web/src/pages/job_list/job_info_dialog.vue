<template>
    <div>
        <el-dialog :title="jobInfoTitle" :visible.sync="isVisible" :before-close="closeDialog" v-loading="loading" element-loading-text="请稍等···">
            <el-form :model="jobInfo" :rules="rules" ref="jobInfo" label-width="180px">
                <el-form-item label="作业类型" prop="jobType">
                    <el-col :span="18">
                        <el-select v-model="jobInfo.jobType" style="width: 100%" :disabled="!isEditable">
                            <el-option v-for="item in $option.jobTypes" :label="item.label" :value="item.value" :key="item.value"></el-option>
                        </el-select>
                    </el-col>
                </el-form-item>
                <el-form-item label="作业名" prop="jobName">
                    <el-col :span="18">
                        <el-input v-model="jobInfo.jobName" placeholder="如SaturnJavaJob"></el-input>
                    </el-col>
                </el-form-item>
                <el-form-item label="作业实现类" prop="jobClass" v-if="jobInfo.jobType !== 'SHELL_JOB'">
                    <el-col :span="18">
                        <el-input v-model="jobInfo.jobClass" placeholder="如com.vip.saturn.job.SaturnJavaJob"></el-input>
                    </el-col>
                </el-form-item>
                <el-form-item label="cron表达式" prop="cron" v-if="jobInfo.jobType !== 'MSG_JOB'">
                    <el-col :span="18">
                        <el-tooltip popper-class="form-tooltip" content="作业启动时间的cron表达式。如每10秒运行:*/10****?,每5分钟运行:0*/5***?" placement="bottom">
                            <el-input v-model="jobInfo.cron">
                                <el-button slot="append" @click="checkAndForecastCron">预测</el-button>
                            </el-input>
                        </el-tooltip>
                    </el-col>
                </el-form-item>
                <el-form-item class="form-annotation" v-if="jobInfo.jobType !== 'MSG_JOB'">
                    <span>1. 每10秒运行一次的表达式：*/10 * * * * ?</span><br/>
                    <span>2. 每分钟运行一次的表达式：0 * * * * ?</span>
                </el-form-item>
                <el-form-item label="作业分片总数" prop="shardingTotalCount">
                    <el-col :span="18">
                        <el-input-number v-model="jobInfo.shardingTotalCount" controls-position="right" :min="1" style="width: 100%;"></el-input-number>
                    </el-col>
                </el-form-item>
                <el-form-item label="分片参数" prop="shardingItemParameters">
                    <el-col :span="18">
                        <el-tooltip popper-class="form-tooltip" placement="bottom">
                            <div slot="content">
                                分片序列号和参数用等号分隔，多个键值对用逗号分隔 。分片序列号从0开始，不可大于或等于作业分片总数。如：0=a,1=b,2=c;<br/>
                                英文双引号请使用!!代替，英文等号请使用@@代替，英文逗号请使用##代替。<br/>
                                如果作业所有分片无须参数，则只要保持值为0。例如有2个分片无须参数，则为“0=0”。<br/>
                                对于本地模式的作业，格式为*=value
                            </div>
                            <el-input type="textarea" v-model="jobInfo.shardingItemParameters"></el-input>
                        </el-tooltip>
                    </el-col>
                </el-form-item>
                <el-form-item label="自定义参数" prop="jobParameter">
                    <el-col :span="18">
                        <el-tooltip popper-class="form-tooltip" content="配置格式: 多个配置使用逗号分隔(key1=value1, key2=value2)。在分片序列号/参数对照表中可作为alias形式引用，格式为{key1}" placement="bottom">
                            <el-input type="textarea" v-model="jobInfo.jobParameter"></el-input>
                        </el-tooltip>
                    </el-col>
                </el-form-item>
                <el-form-item prop="timeZone" label="时区">
                    <el-col :span="18">
                        <el-select filterable v-model="jobInfo.timeZone" style="width: 100%">
                            <el-option v-for="item in timeZonesArray" :label="item" :value="item" :key="item"></el-option>
                        </el-select>
                    </el-col>
                </el-form-item>
                <el-form-item label="Queue名" prop="queueName" v-if="jobInfo.jobType === 'MSG_JOB'">
                    <el-col :span="18">
                        <el-tooltip popper-class="form-tooltip" placement="bottom">
                            <div slot="content">
                                消息类型job的queue名，2.0.0版本的executor支持为每个分片配置一个queue，格式为0=queue1,1=queue2
                            </div>
                            <el-input type="textarea" v-model="jobInfo.queueName"></el-input>
                        </el-tooltip>
                    </el-col>
                </el-form-item>
                <el-form-item label="描述" prop="description">
                    <el-col :span="18">
                        <el-input type="textarea" v-model="jobInfo.description"></el-input>
                    </el-col>
                </el-form-item>
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
      timeZonesArray: [],
      rules: {
        jobType: [{ required: true, message: '请选择作业类型', trigger: 'change' }],
        jobName: [{ required: true, message: '作业名不能为空', trigger: 'blur' }],
        jobClass: [{ required: true, message: '作业实现类不能为空', trigger: 'blur' }],
        cron: [{ required: true, message: 'cron表达式不能为空', trigger: 'blur' }],
        shardingItemParameters: [{ required: true, message: '分片序列号/参数对照表不能为空', trigger: 'blur' }],
      },
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
    getTimeZones() {
      this.$http.get('/console/utils/timeZones').then((data) => {
        this.timeZonesArray = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取时区请求失败！'); });
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
  },
  computed: {
    isEditable() {
      return this.jobInfoOperation !== 'copy';
    },
  },
  created() {
    this.getTimeZones();
  },
};
</script>
