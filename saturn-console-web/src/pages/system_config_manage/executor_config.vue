<template>
    <div class="margin-20"  v-loading="loading" element-loading-text="请稍等···">
        <el-collapse v-model="activeNames">
            <el-collapse-item name="executor">
                <template slot="title"><i class="fa fa-list-alt"></i>Executor配置</template>
                <div>
                    <config-input :parameters="executorConfig.job_configs"></config-input>
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
      activeNames: ['executor'],
      executorConfig: {},
    };
  },
  methods: {
  },
  created() {
  },
  components: {
    'config-input': configInput,
  },
};
</script>
<style lang="sass">
</style>
