<template>
    <el-dialog title="预测" width="500px" :visible.sync="isVisible" :before-close="closeDialog">
        <el-form label-width="180px">
            <el-form-item label="检验结果">
                <el-col :span="20">
                    <el-tag :type="result === '成功' ? 'success': 'danger' " class="form-tags">{{result}}</el-tag>
                </el-col>
            </el-form-item>
            <el-form-item label="作业时区">
                <el-col :span="20">
                    {{cronPredictInfo.timeZone}}
                </el-col>
            </el-form-item>
            <el-form-item label="预测执行时间点">
                <el-tag class="form-tags" type="" v-for="item in cronPredictInfo.nextFireTimes" :key="item">{{item}}</el-tag>
            </el-form-item>
        </el-form>
    </el-dialog>
</template>

<script>
export default {
  props: ['cronPredictParams'],
  data() {
    return {
      isVisible: true,
      cronPredictInfo: {},
      result: '失败',
    };
  },
  methods: {
    closeDialog() {
      this.$emit('close-dialog');
    },
    checkAndForecastCron() {
      this.$http.post('/console/utils/checkAndForecastCron', this.cronPredictParams).then((data) => {
        this.cronPredictInfo = data;
        this.result = '成功';
      })
      .catch(() => { this.$http.buildErrorHandler('预测请求失败！'); });
    },
  },
  created() {
    this.checkAndForecastCron();
  },
};
</script>
<style lang="sass" scoped>
</style>
