<template>
    <div class="margin-20"  v-loading="loading" element-loading-text="请稍等···">
        <el-collapse v-model="activeNames">
            <el-collapse-item name="1">
                <template slot="title"><i class="fa fa-list-alt"></i>作业配置</template>
                <div>
                    <config-input :parameters="systemConfig.job_configs" @config-input-success="configInputSuccess"></config-input>
                </div>
            </el-collapse-item>
            <el-collapse-item name="2">
                <template slot="title"><i class="fa fa-area-chart"></i>Executor配置</template>
                <div>
                    <config-input :parameters="systemConfig.executor_configs" @config-input-success="configInputSuccess"></config-input>
                </div>
            </el-collapse-item>
            <el-collapse-item name="3">
                <template slot="title"><i class="fa fa-sitemap"></i>ZK集群配置</template>
                <div>
                    <config-input :parameters="systemConfig.cluster_configs" @config-input-success="configInputSuccess"></config-input>
                </div>
            </el-collapse-item>
        </el-collapse>
    </div>
</template>
<script>
import configInput from './config_input';

export default {
  props: [],
  data() {
    return {
      loading: false,
      activeNames: ['1', '2', '3'],
      systemConfig: {},
    };
  },
  methods: {
    getSystemConfig() {
      this.loading = true;
      this.$http.get('/console/configs').then((data) => {
        this.systemConfig = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取系统配置请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    configInputSuccess() {
      this.$message.successNotify('更新成功');
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
