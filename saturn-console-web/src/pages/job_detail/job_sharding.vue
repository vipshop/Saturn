<template>
    <div class="page-content">
        <div class="page-table">
            <div class="page-table-header">
                <div class="page-table-header-title"><i class="fa fa-list"></i>分片情况
                    <el-button type="text" @click="getJobSharding"><i class="fa fa-refresh"></i></el-button>
                </div>
            </div>
            <el-table stripe border :data="haveShardJobShardings" style="width: 100%">
                <el-table-column prop="executorName" label="Executor" min-width="110px">
                    <template slot-scope="scope">
                        <i class="iconfont icon-docker" v-if="scope.row.container"></i>
                        <i class="iconfont icon-zhuji" v-if="!scope.row.container"></i>
                        <el-tooltip content="leader" placement="top" v-if="scope.row.leader">
                            <i class="fa fa-flag" style="color: red;"></i>
                        </el-tooltip>
                        {{scope.row.executorName}}
                    </template>
                </el-table-column>
                <el-table-column prop="ip" label="IP" width="130px"></el-table-column>
                <el-table-column label="分片项">
                    <template slot-scope="scope">
                        <el-tag class="sharding-tag" type="primary" v-for="item in $array.strToArray(scope.row.sharding)" :key="item">{{item}}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="分片总数" width="90px">
                    <template slot-scope="scope">
                        {{$array.strToArray(scope.row.sharding).length}}
                    </template>
                </el-table-column>
                <el-table-column label="最近处理数(每天)">
                    <template slot-scope="scope">
                        <el-tag type="success">成功:{{scope.row.processSuccessCount}}</el-tag>
                        <el-tag type="danger">失败:{{scope.row.processFailureCount}}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="version" label="Executor版本"></el-table-column>
            </el-table>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      jobShardings: [],
    };
  },
  methods: {
    getJobSharding() {
      this.$http.get(`/console/namespaces/${this.domainName}/jobs/${this.jobName}/sharding/status`).then((data) => {
        this.jobShardings = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取分片情况请求失败！'); });
    },
  },
  computed: {
    haveShardJobShardings() {
      const arr = [];
      this.jobShardings.forEach((ele) => {
        if (ele.sharding !== '') {
          arr.push(ele);
        }
      });
      return arr;
    },
    domainName() {
      return this.$route.params.domain;
    },
    jobName() {
      return this.$route.params.jobName;
    },
  },
  created() {
    this.getJobSharding();
  },
  watch: {
    $route: 'getJobSharding',
  },
};
</script>
<style lang="sass" scoped>
.sharding-tag {
  margin-right: 3px;
}
</style>
