<template>
    <div class="page-content">
        <div class="page-table">
            <div class="page-table-header">
                <div class="page-table-header-title"><i class="fa fa-list"></i>分片情况
                    <el-button type="text" @click="getJobSharding"><i class="fa fa-refresh"></i></el-button>
                </div>
            </div>
            <el-table stripe border :data="jobShardings" style="width: 100%">
                <el-table-column prop="executorName" label="Executor" min-width="120px"></el-table-column>
                <el-table-column prop="ip" label="IP"></el-table-column>
                <el-table-column prop="sharding" label="分片项"></el-table-column>
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
