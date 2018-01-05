<template>
    <div class="page-content">
        <div class="page-table">
            <div class="page-table-header">
                <div class="page-table-header-title"><i class="fa fa-list"></i>分片情况
                    <el-button type="text" @click="getJobSharding"><i class="fa fa-refresh"></i></el-button>
                </div>
            </div>
            <el-table stripe border :data="jobShardings" style="width: 100%">
                <el-table-column prop="executorName" label="Executor" min-width="100px">
                    <template slot-scope="scope">
                        <i class="iconfont icon-docker" v-if="scope.row.container"></i>
                        <i class="iconfont icon-zhuji" v-if="!scope.row.container"></i>
                        {{scope.row.executorName}}
                    </template>
                </el-table-column>
                <el-table-column prop="ip" label="IP"></el-table-column>
                <el-table-column label="分片项">
                    <template slot-scope="scope">
                        <el-tag class="sharding-tag" type="primary" v-for="item in $array.strToArray(scope.row.sharding)" :key="item">{{item}}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="version" label="作业版本"></el-table-column>
            </el-table>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      domainName: this.$route.params.domain,
      jobName: this.$route.params.jobName,
      jobShardings: [],
    };
  },
  methods: {
    getJobSharding() {
      this.$http.get(`/console/${this.domainName}/jobs/${this.jobName}/sharding/status`).then((data) => {
        this.jobShardings = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取分片情况请求失败！'); });
    },
  },
  created() {
    this.getJobSharding();
  },
};
</script>
<style lang="sass" scoped>
.sharding-tag {
  margin-right: 3px;
}
</style>
