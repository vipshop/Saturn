<template>
    <el-dialog title="作业分片分配详情" :visible.sync="isVisible" :before-close="closeDialog">
        <el-form label-width="130px">
            <el-form-item label="Executor">
                <el-col :span="22">
                    <b>{{executorAllocationInfo.executorName}}</b>
                </el-col>
            </el-form-item>
            <el-form-item label="负荷">
                <el-col :span="22">
                    <b>{{executorAllocationInfo.totalLoadLevel}}</b>
                </el-col>
            </el-form-item>
            <el-form-item label="分片分布">
                <el-col :span="22">
                    <div v-if="executorAllocationInfo.jobStatus.length === 0">无</div>
                    <div v-else>
                        <el-tag :type="statusTag[item.status]" class="form-tags" v-for="item in executorAllocationInfo.jobStatus" :key="item.jobName">{{item.jobName}}({{translateStatus[item.status]}}) : {{item.sharding}}</el-tag>
                    </div>
                </el-col>
            </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">    
            <el-button @click="closeDialog()">取消</el-button>
        </div>
    </el-dialog>
</template>

<script>
export default {
  props: ['executorAllocationInfo'],
  data() {
    return {
      isVisible: true,
      statusTag: {
        READY: 'primary',
        RUNNING: 'success',
        STOPPING: 'warning',
        STOPPED: '',
      },
      translateStatus: {
        READY: '已就绪',
        RUNNING: '运行中',
        STOPPING: '停止中',
        STOPPED: '已停止',
      },
    };
  },
  methods: {
    closeDialog() {
      this.$emit('close-dialog');
    },
  },
};
</script>
<style lang="sass" scoped>
</style>
