<template>
    <el-dialog title="添加配置" width="35%" :visible.sync="isVisible" :before-close="closeDialog" v-loading="loading" element-loading-text="请稍等···">
        <el-form :model="configInfo" :rules="rules" ref="configInfo" label-width="100px">
            <el-form-item label="KEY" prop="key">
                <el-col :span="20">
                    <el-input v-model="configInfo.key"></el-input>
                </el-col>
            </el-form-item>
            <el-form-item label="VALUE" prop="value">
                <el-col :span="20">
                    <el-input v-model="configInfo.value"></el-input>
                </el-col>
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
  props: ['type'],
  data() {
    return {
      isVisible: true,
      loading: false,
      configInfo: {
        key: '',
        value: '',
      },
      rules: {
        key: [{ required: true, message: '请输入key', trigger: 'blur' }],
        value: [{ required: true, message: '请输入value', trigger: 'blur' }],
      },
    };
  },
  methods: {
    handleSubmit() {
      this.$refs.configInfo.validate((valid) => {
        if (valid) {
          const url = this.type === 'console' ? `/console/configs/${this.type}/create` : `/console/configs/${this.type}`;
          this.$http.post(url, this.configInfo).then(() => {
            this.$emit('add-config-success');
          })
          .catch(() => { this.$http.buildErrorHandler('添加系统配置请求失败！'); });
        }
      });
    },
    closeDialog() {
      this.$emit('close-dialog');
    },
  },
  computed: {
  },
};
</script>
