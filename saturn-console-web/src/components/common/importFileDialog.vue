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
                        :action="importUrl"
                        :data= "importData"
                        :auto-upload="false"
                        :multiple="false"
                        :on-error="handleError"
                        :on-success="handleSuccess">
                        <el-button slot="trigger" size="small" type="primary" @click="handleUpload">选取文件</el-button>
                        <div slot="tip" class="el-upload__tip">{{importTip}}</div>
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
  props: ['importTitle', 'importUrl', 'importTemplateUrl', 'importData', 'importTip'],
  data() {
    return {
      isVisible: true,
    };
  },
  methods: {
    exportModel() {
      window.location.href = this.importTemplateUrl;
    },
    handleUpload() {
      this.$refs.upload.clearFiles();
    },
    handleError(err) {
      this.$message.errorMessage(`上传失败: ${err}`);
    },
    handleSuccess(response) {
      if (response.status === 0) {
        this.$emit('import-success', response.obj);
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
