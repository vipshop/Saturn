<template>
    <el-dialog title="添加新域" width="35%" :visible.sync="isVisible" :before-close="closeDialog" v-loading="loading" element-loading-text="请稍等···">
        <el-form :model="namespaceInfo" :rules="rules" ref="namespaceInfo" label-width="100px">
            <el-form-item label="zk集群" prop="zkClusterKey">
                <el-col :span="20">
                    <el-select size="small" v-model="namespaceInfo.zkClusterKey" style="width: 100%">
                        <el-option v-for="item in onLineClusters" :label="item.zkAlias" :value="item.zkClusterKey" :key="item.zkClusterKey"></el-option>
                    </el-select>
                </el-col>
            </el-form-item>
            <el-form-item label="域名" prop="namespace">
                <el-col :span="20">
                    <el-input v-model="namespaceInfo.namespace"></el-input>
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
  props: ['namespaceInfo', 'zkClusterKeys'],
  data() {
    return {
      isVisible: true,
      loading: false,
      rules: {
        zkClusterKey: [{ required: true, message: '请选择zk集群', trigger: 'change' }],
        namespace: [{ required: true, message: '请输入域名', trigger: 'blur' }],
      },
    };
  },
  methods: {
    handleSubmit() {
      this.$refs.namespaceInfo.validate((valid) => {
        if (valid) {
          this.namespaceInfoRequest('/console/namespaces');
        }
      });
    },
    namespaceInfoRequest(url) {
      this.$http.post(url, this.namespaceInfo).then(() => {
        this.loading = true;
        setTimeout(() => {
          this.loading = false;
          this.$emit('namespace-info-success');
        }, 3000);
      })
      .catch(() => { this.$http.buildErrorHandler(`${url}请求失败！`); });
    },
    closeDialog() {
      this.$emit('close-dialog');
    },
  },
  computed: {
    onLineClusters() {
      const onlineArr = [];
      this.zkClusterKeys.forEach((element) => {
        if (!element.offline) {
          onlineArr.push(element);
        }
      });
      return onlineArr;
    },
  },
};
</script>
