<template>
    <div v-loading.fullscreen="loading" element-loading-text="请稍等···">
        <div class="top-bar">
            <div class="top-bar-left">
                <i class="fa fa-home"></i>
                <span>当前域: 
                    <router-link tag="a" :to="{ name: 'job_overview', params: { domain: domain } }">{{this.domain}}</router-link>
                </span>
                <span>所属集群: <router-link tag="a" :to="{ name: 'clusters_manage' }">{{domainInfo.zkAlias}}</router-link></span>
                <el-tooltip placement="right">
                    <div slot="content">
                        运维负责人：<span>{{domainInfo.sysAdmin || '空'}}</span><br/>
                        开发负责人：<span>{{domainInfo.techAdmin || '空'}}</span>
                    </div>
                    <i class="el-icon-warning"></i>
                </el-tooltip>
            </div>
            <div class="top-bar-right">
                <el-autocomplete
                    v-if="isShowBarSearch"
                    prefix-icon="el-icon-search"
                    v-model="jobNameSelect"
                    :fetch-suggestions="querySearchAsync"
                    placeholder="切换作业"
                    popper-class="header-autocomplete"
                    class="top-bar-search"
                    @select="handleSelect">
                </el-autocomplete>
                <Favorites></Favorites>
            </div>
        </div>
    </div>
</template>
<script>
export default {
  props: ['domain', 'domainInfo'],
  data() {
    return {
      jobNameSelect: '',
      loading: false,
      jobNameList: [],
    };
  },
  methods: {
    querySearchAsync(queryString, cb) {
      const jobNameList = this.jobNameList;
      const results = queryString ?
      jobNameList.filter(this.createStateFilter(queryString)) : jobNameList;
      cb(results);
    },
    createStateFilter(queryString) {
      return state =>
        state.value.toLowerCase().indexOf(queryString.toLowerCase()) >= 0;
    },
    handleSelect(item) {
      this.toPage(item.value);
    },
    toPage(jobName) {
      this.jobNameSelect = '';
      this.$router.push({ name: 'job_setting', params: { domain: this.domain, jobName } });
    },
    getJobNameList() {
      return this.$http.get(`/console/namespaces/${this.domain}/jobs/names`).then((data) => {
        this.jobNameList = data.map((obj) => {
          const rObj = {};
          rObj.value = obj;
          return rObj;
        });
      })
      .catch(() => { this.$http.buildErrorHandler('获取作业名列表失败！'); });
    },
    init() {
      this.loading = true;
      Promise.all([this.getJobNameList()]).then(() => {
        this.loading = false;
      });
    },
  },
  computed: {
    isShowBarSearch() {
      let flag = false;
      const pathArr = this.$route.path.split('/');
      if (pathArr[1] === 'job_detail') {
        flag = true;
      }
      return flag;
    },
  },
  created() {
    this.init();
  },
};
</script>
<style lang="sass">
.top-bar {
    padding: 0 30px;
    height: 40px;
    background: #38465a;
    font-size: 15px;
    line-height: 40px;
    color: #97a8be;
    .top-bar-left {
        float: left;
        >span {
            margin-right: 15px;
            a {
                color: #c2d0d7;
                &:hover {
                    color: #fff;
                }
            }
        }
    }
    .top-bar-right {
        float: right;
        .el-button--text {
            .fa {
                font-size: 16px;
                color: #c2d0d7;
            }
        }
    }
}
.top-bar-search {
    margin-right: 5px;
    .el-input__inner {
        border: none;
        background-color: #2b3846;
        color: #c2d0d7;
    }
    .el-input__prefix {
        color: #606f7e;
    }
    input::-webkit-input-placeholder {  
        color: #606f7e;   
    } 
}
</style>
