<template>
    <div class="page-content">
        <el-form :model="jobSettingInfo" :rules="rules" ref="jobSettingInfo" label-width="160px">
            <el-collapse v-model="activeNames">
                <el-collapse-item name="1">
                    <template slot="title">
                        基本配置<el-button size="small" type="primary" icon="el-icon-refresh" style="margin-left: 20px;">更新</el-button>
                    </template>
                    <div>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="jobClass" label="作业实现类" required>
                                    <el-input v-model="jobSettingInfo.jobClass"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="8">
                                <el-form-item prop="cron" label="Cron">
                                    <el-input v-model="jobSettingInfo.cron"></el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="2">
                                <el-form-item label-width="10">
                                    <a>预测</a>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="8">
                                <el-form-item prop="shardNum" label="作业分片数">
                                    <el-input v-model="jobSettingInfo.shardNum"></el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="16">
                                <el-form-item prop="localMode" label="本地模式">
                                    <el-switch v-model="jobSettingInfo.localMode"></el-switch>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="abc" label="分片序列号/参数对照表">
                                    <el-input type="textarea" v-model="jobSettingInfo.abc"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="customParams" label="自定义参数">
                                    <el-input type="textarea" v-model="jobSettingInfo.customParams"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="8">
                                <el-form-item prop="executor" label="优先executor">
                                    <el-input v-model="jobSettingInfo.executor"></el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="16">
                                <el-form-item prop="onlyExecutor" label="只使用优先executor">
                                    <el-switch v-model="jobSettingInfo.localMode"></el-switch>
                                </el-form-item>
                            </el-col>
                        </el-row>
                    </div>
                </el-collapse-item>
                <el-collapse-item name="2">
                    <template slot="title">高级配置</template>
                    <div>
                        <el-row>
                            <el-col :span="11">
                                <el-form-item prop="timeoutAlarm" label="超时告警(秒)">
                                    <el-input v-model="jobSettingInfo.timeoutAlarm"></el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11">
                                <el-form-item prop="timeoutKill" label="超时强杀(秒)">
                                    <el-input v-model="jobSettingInfo.timeoutKill"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="22">
                                <el-form-item prop="group" label="所属分组">
                                    <el-input v-model="jobSettingInfo.group"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="11">
                                <el-form-item prop="load" label="作业负荷">
                                    <el-input v-model="jobSettingInfo.load"></el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11">
                                <el-form-item prop="processInteval" label="统计处理间隔">
                                    <el-input v-model="jobSettingInfo.processInteval"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="11">
                                <el-form-item prop="timeZone" label="时区">
                                    <el-input v-model="jobSettingInfo.timeZone"></el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11">
                                <el-form-item prop="dependJob" label="依赖作业">
                                    <el-input v-model="jobSettingInfo.dependJob"></el-input>
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row>
                            <el-col :span="11">
                                <el-form-item prop="consoleOutputLog" label="显示控制台输出日志">
                                    <el-switch v-model="jobSettingInfo.consoleOutputLog"></el-switch>
                                </el-form-item>
                            </el-col>
                            <el-col :span="11">
                                <el-form-item prop="operateStatus" label="上报运行状态">
                                    <el-switch v-model="jobSettingInfo.operateStatus"></el-switch>
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
  data() {
    return {
      activeNames: ['1'],
      jobSettingInfo: {
        jobClass: '',
        cron: '',
        shardNum: '',
        localMode: true,
        abc: '',
        customParams: '',
        executor: '',
        onlyExecutor: true,
        timeoutAlarm: 10,
        timeoutKill: 10,
        group: '',
        load: 2,
        processInteval: 200,
        timeZone: '',
        dependJob: '',
        consoleOutputLog: true,
        operateStatus: false,
      },
      rules: {
        jobClass: [{ required: true, message: '作业实现类不能为空', trigger: 'blur' }],
      },
    };
  },
};
</script>
