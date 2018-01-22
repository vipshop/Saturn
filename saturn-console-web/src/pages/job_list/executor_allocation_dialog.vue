<template>
    <el-dialog title="作业分片分配详情" :visible.sync="isVisible" :before-close="closeDialog">
        <el-form label-width="140px">
            <el-form-item label="Executor">
                <el-col :span="18">
                    {{executorAllocationInfo.executorName}}
                </el-col>
            </el-form-item>
            <el-form-item label="负荷">
                <el-col :span="18">
                    {{executorAllocationInfo.totalLoadLevel}}
                </el-col>
            </el-form-item>
            <el-form-item label="分片分布">
                <el-col :span="18">
                    <div v-if="Object.entries(executorAllocationInfo.allocationMap).length === 0">无</div>
                    <div v-else>
                        <el-tag type="success" class="form-tags" v-for="item in Object.entries(executorAllocationInfo.allocationMap)" :key="item[0]">{{item[0]}}:{{item[1]}}</el-tag>
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
