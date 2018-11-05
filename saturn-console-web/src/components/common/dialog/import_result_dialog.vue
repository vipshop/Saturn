<template>
    <el-dialog title="导入作业结果" width="45%" :visible.sync="isVisible" :before-close="closeDialog" :close-on-click-modal="false" :close-on-press-escape="false" :show-close="false">
        <el-table stripe border :data="importResult" style="width: 100%">
            <el-table-column prop="jobName" label="作业名称"></el-table-column>
            <el-table-column prop="success" label="状态" width="70px">
                <template slot-scope="scope">
                    <el-tag :type="scope.row.success ? 'success' : 'danger'">{{statusMap[scope.row.success]}}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="message" label="失败详情" min-width="150px"></el-table-column>
        </el-table>
        <div slot="footer" class="dialog-footer">
            <el-button type="primary" @click="closeDialog()">确认</el-button>
        </div>
    </el-dialog>
</template>

<script>
export default {
  props: ['importResult'],
  data() {
    return {
      isVisible: true,
      statusMap: {
        true: '成功',
        false: '失败',
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
