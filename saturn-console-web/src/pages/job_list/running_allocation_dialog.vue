<template>
    <el-dialog title="运行中作业分片查看" :visible.sync="isVisible" :before-close="closeDialog">
        <el-form label-width="190px">
            <el-form-item label="Executor">
                <el-col :span="22">
                    <b>{{runningAllocationInfo.executorName}}</b>
                </el-col>
            </el-form-item>
            <el-form-item label="运行中作业分片">
                <el-col :span="22">
                    <div v-if="Object.entries(runningAllocationInfo.runningJobItems).length === 0">无</div>
                    <div v-else>
                        <el-tag class="form-tags" type="success" v-for="item in Object.entries(runningAllocationInfo.runningJobItems)" :key="item[0]">{{item[0]}} : {{item[1]}}</el-tag>
                    </div>
                </el-col>
            </el-form-item>
            <el-form-item>
                <div slot="label">
                    <span>可能运行中的作业分片</span>
                    <el-tooltip placement="top" content="作业状态未上报，无法获知运行状态，例如消息作业和秒级作业" effect="light" popper-class="allocation-popper">
                        <i class="fa fa-question-circle"></i>
                    </el-tooltip>
                </div>
                <el-col :span="22">
                    <div v-if="Object.entries(runningAllocationInfo.potentialRunningJobItems).length === 0">无</div>
                    <div v-else>
                        <el-tag class="form-tags" type="warning" v-for="item in Object.entries(runningAllocationInfo.potentialRunningJobItems)" :key="item[0]">{{item[0]}} : {{item[1]}}</el-tag>
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
  props: ['runningAllocationInfo'],
  data() {
    return {
      isVisible: true,
    };
  },
  methods: {
    closeDialog() {
      this.$emit('close-dialog');
    },
  },
};
</script>
