<template>
    <div class="margin-20"  v-loading="loading" element-loading-text="请稍等···">
        <div>
            <el-button type="primary" style="margin-bottom: 10px;" @click="handleAdd">添加配置</el-button>
        </div>
        <el-collapse v-model="activeNames">
            <el-collapse-item name="executor">
                <template slot="title"><i class="fa fa-list-alt"></i>Executor配置</template>
                <div>
                    <config-input type="executor" :parameters="executorConfig" @config-input-success="configInputSuccess"></config-input>
                </div>
            </el-collapse-item>
        </el-collapse>
        <div v-if="addConfigVisible">
            <add-config-dialog type="executor" @close-dialog="closeDialog" @add-config-success="addConfigSuccess"></add-config-dialog>
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
      activeNames: ['executor'],
      executorConfig: [],
    };
  },
  methods: {
    handleAdd() {
      this.addConfigVisible = true;
    },
    closeDialog() {
      this.addConfigVisible = false;
    },
    addConfigSuccess() {
      this.addConfigVisible = false;
      this.$message.successNotify('添加Executor配置成功');
      this.getExecutorConfig();
    },
    getExecutorConfig() {
      this.loading = true;
      this.$http.get('/console/configs/executor').then((data) => {
        this.executorConfig = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取Executor配置请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    configInputSuccess() {
      this.$message.successNotify('Executor配置保存成功');
      this.getExecutorConfig();
    },
  },
  created() {
    this.getExecutorConfig();
  },
  components: {
    'config-input': configInput,
  },
};
</script>
<style lang="sass">
</style>
