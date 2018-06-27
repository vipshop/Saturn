<template>
    <div>
        <div class="param-table">
            <el-table :data="parameters" border style="width: 100%">
                <el-table-column label="配置项" header-align="left">
                    <template slot-scope="scope">
                        <el-tooltip popper-class="form-tooltip" placement="bottom">
                            <div slot="content">
                                <span v-if="scope.row.description">{{scope.row.description}}</span>
                                <span v-else>暂无描述</span>
                            </div>
                            <span>{{scope.row.key}}</span>
                        </el-tooltip>
                    </template>
                </el-table-column>
                <el-table-column label="值" header-align="left">
                    <template slot-scope="scope">
                        <el-input v-model="scope.row.value"></el-input>
                    </template>
                </el-table-column>
                <el-table-column label="操作" header-align="left" width="80px" align="center">
                    <template slot-scope="scope">
                        <el-button size="small" type="primary" @click="handleSubmit(scope.row)">更新</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>
    </div>
</template>

<script>

export default {
  props: ['parameters', 'type'],
  data() {
    return {
    };
  },
  methods: {
    handleSubmit(row) {
      const url = this.type === 'console' ? `/console/configs/${this.type}/update` : `/console/configs/${this.type}`;
      const params = {
        key: row.key,
        value: row.value,
      };
      this.$http.post(url, params).then(() => {
        this.$emit('config-input-success');
      })
      .catch(() => { this.$http.buildErrorHandler('更新配置请求失败！'); });
    },
  },
};

</script>

<style type="text/css">
.param-table {
    margin: 0 10px;
}
.param-table .el-table th>.cell {
    line-height: 15px;
}
.param-table .el-table tr:hover {
    background-color: initial;
}
.param-add-button {
    margin: 5px 0;
}
</style>
