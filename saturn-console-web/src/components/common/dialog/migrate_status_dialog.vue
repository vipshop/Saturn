<template>
    <el-dialog :title="migrateStatusTitle" width="45%" :visible.sync="isVisible" :before-close="closeDialog" :close-on-click-modal="false" :close-on-press-escape="false" :show-close="false">
        <el-form :model="migrateStatusInfo" ref="migrateStatusInfo" label-width="80px">
            <el-form-item label="迁移量">
                <el-col :span="20">
                    <el-tag type="primary" class="form-tags">总共 : {{migrateStatusInfo.totalCount}}</el-tag>
                    <el-tag type="success" class="form-tags">成功 : {{migrateStatusInfo.successCount}}</el-tag>
                    <el-tag type="danger" class="form-tags">失败 : {{migrateStatusInfo.failCount}}</el-tag>
                    <el-tag type="warning" class="form-tags">忽略 : {{migrateStatusInfo.ignoreCount}}</el-tag>
                    <el-tag type="" class="form-tags">未做 : {{migrateStatusInfo.unDoCount}}</el-tag>
                </el-col>
            </el-form-item>
            <el-form-item label="正在迁移">
                <el-col :span="20">
                    <el-input type="textarea" v-model="migrateStatusInfo.moving" readonly></el-input>
                </el-col>
            </el-form-item>
            <el-form-item>
                <div>注：忽略的域，指该域已经处于目标集群</div>
            </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
            <el-button type="primary" @click="closeDialog()" :disabled="!migrateStatusInfo.finished">完成</el-button>
        </div>
    </el-dialog>
</template>

<script>
export default {
  props: ['migrateStatusTitle'],
  data() {
    return {
      isVisible: false,
      migrateStatusInfo: {
        failCount: 0,
        ignoreCount: 0,
        successCount: 0,
        totalCount: 0,
        unDoCount: 0,
        finished: false,
        moving: '',
      },
    };
  },
  methods: {
    closeDialog() {
      this.$emit('close-dialog');
    },
    getMigrationStatus() {
      this.$http.get('/console/namespaces/zkCluster/migrationStatus').then((data) => {
        this.migrateStatusInfo = data;
        this.isVisible = true;
        if (!data.finished) {
          this.getMigrationStatus();
        }
      })
      .catch(() => { this.$http.buildErrorHandler('获取迁移结果状态请求失败！'); });
    },
  },
  created() {
    this.getMigrationStatus();
  },
};
</script>
