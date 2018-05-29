<template>
    <el-dialog :title="clusterInfoTitle" width="35%" :visible.sync="isVisible" :before-close="closeDialog" v-loading="loading" element-loading-text="请稍等···">
        <el-form :model="clusterInfo" :rules="rules" ref="clusterInfo" label-width="100px">
            <el-form-item label="集群ID" prop="zkClusterKey">
                <el-col :span="20">
                    <el-input v-model="clusterInfo.zkClusterKey" :disabled="!isEditable"></el-input>
                </el-col>
            </el-form-item>
            <el-form-item label="集群名称" prop="alias">
                <el-col :span="20">
                    <el-input v-model="clusterInfo.alias" :disabled="!isEditable"></el-input>
                </el-col>
            </el-form-item>
            <el-form-item label="连接串" prop="connectString">
                <el-col :span="20">
                    <el-input type="textarea" v-model="clusterInfo.connectString" placeholder="连接串用','分隔"></el-input>
                </el-col>
            </el-form-item>
            <el-form-item class="form-annotation">
                <span>格式: 例如192.168.0.1:2181,192.168.0.2:2181</span>
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
  props: ['clusterInfo', 'clusterInfoTitle', 'clusterInfoOperate'],
  data() {
    return {
      isVisible: true,
      loading: false,
      rules: {
        zkClusterKey: [{ required: true, message: '请输入Zk集群', trigger: 'blur' }],
        alias: [{ required: true, message: '请输入集群名称', trigger: 'blur' }],
        connectString: [{ required: true, message: '请输入ZK连接串', trigger: 'blur' }],
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
      if (this.clusterInfoOperate === 'add') {
        this.loading = true;
        this.$http.post(url, this.clusterInfo).then(() => {
          setTimeout(() => {
            this.loading = false;
            this.$emit('cluster-info-success');
          }, 3000);
        })
        .catch(() => { this.$http.buildErrorHandler(`${url}请求失败！`); })
        .finally(() => {
          this.loading = false;
        });
      } else {
        this.loading = true;
        this.$http.put(url, this.clusterInfo).then(() => {
          setTimeout(() => {
            this.loading = false;
            this.$emit('cluster-info-success');
          }, 3000);
        })
        .catch(() => { this.$http.buildErrorHandler(`${url}请求失败！`); })
        .finally(() => {
          this.loading = false;
        });
      }
    },
    closeDialog() {
      this.$emit('close-dialog');
    },
  },
  computed: {
    isEditable() {
      return this.clusterInfoOperate !== 'edit';
    },
  },
};
</script>
