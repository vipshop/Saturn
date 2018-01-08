<template>
    <div class="page-content">
        <el-form :model="jobSettingInfo" :rules="rules" ref="jobSettingInfo" label-width="140px">
            <el-collapse v-model="activeNames">
                <el-collapse-item name="1">
                    <template slot="title">
                        基本配置<el-button size="small" type="primary" @click.stop="updateInfo" style="margin-left: 20px;"><i class="fa fa-undo"></i>更新</el-button>
                    </template>
                    <div class="job-setting-content">
                        <el-row v-if="jobSettingInfo.jobType === 'JAVA_JOB'">
                            <el-col :span="22">
                                <el-form-item prop="jobClass" label="作业实现类" required>
                                    <el-input v-model="jobSettingInfo.jobClass"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="cron" label="Cron">
                                    <el-tooltip popper-class="form-tooltip" content="作业启动时间的cron表达式。如每10秒运行:*/10****?,每5分钟运行:0*/5***?" placement="bottom">
                                        <el-input v-model="jobSettingInfo.cron"></el-input>
                                    </el-tooltip>
                                </el-form-item>
                            </el-col>
                            <el-col :span="2">
                                <el-form-item label-width="10">
                                    <a>预测</a>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row :gutter="30">
                            <el-col :span="14">
                                <el-form-item prop="shardingTotalCount" label="作业分片数">
                                    <el-input-number v-model="jobSettingInfo.shardingTotalCount" controls-position="right" :min="1" style="width: 100%;"></el-input-number>
                                </el-form-item>
                            </el-col>
                            <el-col :span="8">
                                <el-form-item prop="localMode" label="本地模式">
                                    <el-switch v-model="jobSettingInfo.localMode"></el-switch>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="shardingItemParameters" label="分片序列号/参数对照表">
                                    <el-tooltip popper-class="form-tooltip" content="分片序列号和参数用等号分隔，多个键值对用逗号分隔，类似map。分片序列号从0开始，不可大于或等于作业分片总数。如：0=a,1=b,2=c; 英文双引号请使用!!代替，英文等号请使用@@代替，英文逗号请使用##代替,。特别的，对于本地模式的作业，只需要输入如：*=a，就可以了。" placement="bottom">
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
                                <el-form-item prop="description" label="作业描述信息">
                                    <el-input type="textarea" v-model="jobSettingInfo.description"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row :gutter="30">
                            <el-col :span="14">
                                <el-form-item prop="preferList" label="优先executor">
                                    <el-select size="small" filterable multiple v-model="jobSettingInfo.preferList" style="width: 100%;">
                                        <el-option v-for="item in preferListArray" :label="item.executorName" :value="item.executorName" :key="item.executorName">
                                            <span style="float: left">{{ item.executorName }}</span>
                                            <span style="float: left">({{ statusOnline[item.type] }})</span>
                                        </el-option>
                                    </el-select>
                                </el-form-item>
                            </el-col>
                            <el-col :span="8">
                                <el-form-item prop="useDispreferList" label="只使用优先executor">
                                    <el-switch v-model="jobSettingInfo.useDispreferList"></el-switch>
                                </el-form-item>
                            </el-col>
                        </el-row>
                    </div>
                </el-collapse-item>
                <el-collapse-item name="2">
                    <template slot="title">高级配置</template>
                    <div class="job-setting-content">
                        <el-row>
                            <el-col :span="11">
                                <el-form-item prop="timeout4AlarmSeconds" label="超时告警(秒)">
                                    <el-input v-model="jobSettingInfo.timeout4AlarmSeconds"></el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11">
                                <el-form-item prop="timeoutSeconds" label="超时强杀(秒)">
                                    <el-input v-model="jobSettingInfo.timeoutSeconds"></el-input>
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
                                <el-form-item prop="processCountIntervalSeconds" label="统计处理间隔">
                                    <el-input v-model="jobSettingInfo.processCountIntervalSeconds"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="11">
                                <el-form-item prop="timeZone" label="时区">
                                    <el-select size="small" filterable v-model="jobSettingInfo.timeZone">
                                        <el-option v-for="item in timeZones" :label="item" :value="item" :key="item"></el-option>
                                    </el-select>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11">
                                <el-form-item prop="dependencies" label="依赖作业">
                                    <el-select size="small" filterable multiple v-model="jobSettingInfo.dependencies" style="width: 100%;">
                                        <el-option v-for="item in dependenciesArray" :label="item" :value="item" :key="item"> </el-option>
                                    </el-select>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="11">
                                <el-form-item prop="showNormalLog" label="显示控制台输出日志">
                                    <el-switch v-model="jobSettingInfo.showNormalLog"></el-switch>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11">
                                <el-form-item prop="enabledReport" label="上报运行状态">
                                    <el-switch v-model="jobSettingInfo.enabledReport"></el-switch>
                                </el-form-item>
                            </el-col>
                        </el-row>
                    </div>
                </el-collapse-item>
            </el-collapse>
        </el-form>
    </div>
</template>
<script>
export default {
  props: [],
  data() {
    return {
      domainName: this.$route.params.domain,
      jobName: this.$route.params.jobName,
      activeNames: ['1'],
      jobSettingInfo: {},
      preferListArray: [],
      dependenciesArray: [],
      timeZones: [],
      rules: {
        jobClass: [{ required: true, message: '作业实现类不能为空', trigger: 'blur' }],
      },
      statusOnline: {
        ONLINE: '在线',
        OFFLINE: '离线',
      },
    };
  },
  methods: {
    updateInfo() {
      this.$http.post(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/config`, this.jobSettingInfo).then(() => {
        this.getJobSettingInfo();
        this.$message.successNotify('更新作业操作成功');
      })
      .catch(() => { this.$http.buildErrorHandler('更新作业请求失败！'); });
    },
    getJobSettingInfo() {
      this.$http.get(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/config`).then((data) => {
        this.jobSettingInfo = JSON.parse(JSON.stringify(data));
      })
      .catch(() => { this.$http.buildErrorHandler('获取作业信息请求失败！'); });
    },
  },
  created() {
    this.getJobSettingInfo();
  },
};
</script>
<style lang="sass">
.job-setting-content {
    padding: 10px 15% 0;
}
</style>
