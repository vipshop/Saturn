<template>
    <div class="margin-20"  v-loading="loading" element-loading-text="请稍等···">
        <div>
            <el-button type="primary" style="margin-bottom: 10px;" @click="handleAdd">添加配置</el-button>
        </div>
        <el-collapse v-model="activeNames">
            <el-collapse-item name="job">
                <template slot="title"><i class="fa fa-list-alt"></i>作业配置</template>
                <div>
                    <config-input type="console" :parameters="systemConfig.job_configs" @config-input-success="configInputSuccess"></config-input>
                </div>
            </el-collapse-item>
            <el-collapse-item name="executor">
                <template slot="title"><i class="fa fa-area-chart"></i>Executor配置</template>
                <div>
                    <config-input type="console" :parameters="systemConfig.executor_configs" @config-input-success="configInputSuccess"></config-input>
                </div>
            </el-collapse-item>
            <el-collapse-item name="zkCluster">
                <template slot="title"><i class="fa fa-sitemap"></i>ZK集群配置</template>
                <div>
                    <config-input type="console" :parameters="systemConfig.cluster_configs" @config-input-success="configInputSuccess"></config-input>
                </div>
            </el-collapse-item>
            <el-collapse-item name="console">
                <template slot="title"><i class="fa fa-cog"></i>Console配置</template>
                <div>
                    <config-input type="console" :parameters="systemConfig.console_configs" @config-input-success="configInputSuccess"></config-input>
                </div>
            </el-collapse-item>
            <el-collapse-item name="other">
                <template slot="title"><i class="fa fa-cogs"></i>其他配置</template>
                <div>
                    <config-input type="console" :parameters="systemConfig.other_configs" @config-input-success="configInputSuccess"></config-input>
                </div>
            </el-collapse-item>
        </el-collapse>
        <div v-if="addConfigVisible">
            <add-config-dialog type="console" @close-dialog="closeDialog" @add-config-success="addConfigSuccess"></add-config-dialog>
        </div>
    </div>
</template>
<script>
import configInput from './config_input';

export default {
  props: [],
  data() {
    return {
      loading: false,
      addConfigVisible: false,
      activeNames: ['job', 'executor', 'zkCluster', 'console', 'other'],
      systemConfig: {},
    };
  },
  methods: {
    handleAdd() {
      this.addConfigVisible = true;
    },
    closeDialog() {
      this.addConfigVisible = false;
    },
    getSystemConfig() {
      this.loading = true;
      this.$http.get('/console/configs/console').then((data) => {
        this.systemConfig = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取Console配置请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    addConfigSuccess() {
      this.addConfigVisible = false;
      this.$message.successNotify('添加Console配置成功');
      this.getSystemConfig();
    },
    configInputSuccess() {
      this.$message.successNotify('Console配置保存成功');
      this.getSystemConfig();
    },
  },
  created() {
    this.getSystemConfig();
  },
  components: {
    'config-input': configInput,
  },
};
</script>
<style lang="sass">
</style>
