<template>
    <el-dialog title="添加集群" width="35%" :visible.sync="isVisible" :before-close="closeDialog" v-loading="loading" element-loading-text="请稍等···">
        <el-form :model="clusterInfo" :rules="rules" ref="clusterInfo" label-width="100px">
            <el-form-item label="集群ID" prop="zkClusterKey">
                <el-col :span="20">
                    <el-input v-model="clusterInfo.zkClusterKey"></el-input>
                </el-col>
            </el-form-item>
            <el-form-item label="集群名称" prop="alias">
                <el-col :span="20">
                    <el-input v-model="clusterInfo.alias"></el-input>
                </el-col>
            </el-form-item>
            <el-form-item label="连接串" prop="connectString">
                <el-col :span="20">
                    <el-input type="textarea" v-model="clusterInfo.connectString" placeholder="连接串用','分隔"></el-input>
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
  props: ['clusterInfo'],
  data() {
    return {
      isVisible: true,
      loading: false,
      rules: {
        zkClusterKey: [{ required: true, message: '请输入zk集群', trigger: 'blur' }],
        alias: [{ required: true, message: '请输入集群名称', trigger: 'blur' }],
      },
    };
  },
  methods: {
    handleSubmit() {
      this.$refs.clusterInfo.validate((valid) => {
        if (valid) {
          this.clusterInfoRequest('/console/zkClusters');
        }
      });
    },
    clusterInfoRequest(url) {
      this.$http.post(url, this.clusterInfo).then(() => {
        this.$emit('cluster-info-success');
      })
      .catch(() => { this.$http.buildErrorHandler(`${url}请求失败！`); });
    },
    closeDialog() {
      this.$emit('close-dialog');
    },
  },
};
</script>
