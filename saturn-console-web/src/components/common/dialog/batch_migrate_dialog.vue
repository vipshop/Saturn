<template>
    <el-dialog title="批量域迁移" width="45%" :visible.sync="isVisible" :before-close="closeDialog" v-loading="loading" element-loading-text="请稍等···">
        <el-form :model="batchMigrateInfo" :rules="rules" ref="batchMigrateInfo" label-width="100px">
            <el-form-item label="域名" prop="namespacesArray">
                <el-col :span="18">
                    <el-tag type="success" class="form-tags" v-for="item in batchMigrateInfo.namespacesArray" :key="item">{{item}}</el-tag>
                </el-col>
            </el-form-item>
            <el-form-item label="目标集群" prop="zkClusterKeyNew">
                <el-col :span="18">
                    <el-select filterable  size="small" v-model="batchMigrateInfo.zkClusterKeyNew" style="width: 100%">
                        <el-option v-for="item in onLineClusters" :label="item.zkAlias" :value="item.zkClusterKey" :key="item.zkClusterKey"></el-option>
                    </el-select>
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
  props: ['batchMigrateInfo', 'zkClusterKeys'],
  data() {
    return {
      isVisible: true,
      loading: false,
      rules: {
        zkClusterKeyNew: [{ required: true, message: '请选择目标集群', trigger: 'change' }],
      },
    };
  },
  methods: {
    handleSubmit() {
      this.$refs.batchMigrateInfo.validate((valid) => {
        if (valid) {
          this.$message.confirmMessage(`确定将这 ${this.batchMigrateInfo.namespacesArray.length} 个域迁移到 ${this.batchMigrateInfo.zkClusterKeyNew} 吗?`, () => {
            this.migrateRequest();
          });
        }
      });
    },
    migrateRequest() {
      const params = {
        namespaces: this.batchMigrateInfo.namespacesArray.join(','),
        zkClusterKeyNew: this.batchMigrateInfo.zkClusterKeyNew,
      };
      this.$http.post('/console/namespaces/zkCluster/migrate', params).then(() => {
        this.$emit('batch-migrate-complete', this.batchMigrateInfo.zkClusterKeyNew);
      })
      .catch(() => { this.$http.buildErrorHandler('批量域迁移请求失败！'); });
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
