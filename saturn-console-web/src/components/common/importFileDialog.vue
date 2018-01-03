<template>
    <el-dialog :title="importTitle" width="40%" :visible.sync="isVisible" :before-close="closeDialog">
        <el-form label-width="120px">
            <el-form-item label="下载模板文件">
                <a @click="exportModel">点击下载</a> (请勿更改表头数据)
            </el-form-item>
            <el-form-item label="导入文件 ">
                <div>
                    <el-upload
                        ref="upload"
                        action="/console/job-overview/import-jobs"
                        :data= "importData"
                        :auto-upload="false"
                        :multiple="false"
                        :on-error="handleError"
                        :on-success="handleSuccess">
                        <el-button slot="trigger" size="small" type="primary" @click="handleUpload">选取文件</el-button>
                        <div slot="tip" class="el-upload__tip">温馨提示：(Shell消息作业导入要求所有executor版本都是1.1.2及以上)</div>
                    </el-upload>
                </div>
            </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">    
            <el-button @click="closeDialog()">取消</el-button>
            <el-button type="primary" @click="handleSubmit()">确定</el-button>
        </div>
    </el-dialog>
</template>

<script>
export default {
  props: ['importTitle', 'importData'],
  data() {
    return {
      isVisible: true,
    };
  },
  methods: {
    exportModel() {
      window.location.href = '/console/job-overview/export-jobs-template';
    },
    handleUpload() {
      this.$refs.upload.clearFiles();
    },
    handleError(err) {
      this.$message.errorMessage(`上传失败: ${err}`);
    },
    handleSuccess(response) {
      console.log(response);
      if (response.success) {
        this.$emit('import-success');
      } else {
        this.$message.errorMessage(response.message);
      }
    },
    handleSubmit() {
      this.$refs.upload.submit();
    },
    closeDialog() {
      this.$emit('close-dialog');
      this.$refs.upload.clearFiles();
    },
  },
};
</script>
