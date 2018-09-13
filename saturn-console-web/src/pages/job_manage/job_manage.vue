<template>
    <div style="height: 100%">
        <el-main>
            <div class="page-home">
                <div class="page-home-container">
                    <div class="page-title">
                        <img width="200px" height="200px" src="../../image/saturn.png"/>
                    </div>
                    <div>
                        <el-autocomplete
                            v-model="domainName"
                            :fetch-suggestions="querySearchAsync"
                            placeholder="请输入域名"
                            @select="handleSelect"
                            style="width: 600px;">
                        <i class="el-icon-search el-input__icon" slot="suffix"></i>
                        </el-autocomplete>
                    </div>
                </div>
                <form v-if="recentVisitDomains" class="recent-visit-form">
                    <fieldset class="quick-access">
                        <legend class="quick-access-title">---&nbsp;快捷访问&nbsp;---</legend>
                        <div class="tag-content">
                            <span class="status-tag" v-for="(item, key) in recentVisitDomains" :key="key" @click="toJobListPage(item.params.domain)">
                                {{item.params.domain}}
                            </span>
                        </div>
                    </fieldset>
                </form>
            </div>
        </el-main>
    </div>
</template>

<script>
export default {
  data() {
    return {
      domainName: '',
      domains: this.$store.getters.allDomains,
    };
  },
  methods: {
    querySearchAsync(queryString, cb) {
      const domains = this.domains;
      const results = queryString ? domains.filter(this.createStateFilter(queryString)) : domains;
      cb(results);
    },
    createStateFilter(queryString) {
      return state =>
        state.value.toLowerCase().indexOf(queryString.toLowerCase()) >= 0;
    },
    handleSelect(item) {
      this.toJobListPage(item.value);
    },
    toJobListPage(domain) {
      this.$router.push({ name: 'job_overview', params: { domain } });
    },
  },
  computed: {
    userInfo() {
      return this.$store.state.global.userAuthority;
    },
    recentVisitDomains() {
      return JSON.parse(localStorage.getItem(`history_${this.userInfo.username}`));
    },
  },
};
</script>
<style lang="sass">
.page-home {
    margin: 0 auto;
    max-width: 1200px;
    height: 100%;
    .page-home-container {
        position: relative;
        width: 600px;
        margin: 0 auto;
        .page-title {
            font-size: 19px;
            font-weight: normal;
            margin: 0;
            text-align: center;
            color: #159e74;
        }
    }
    @media screen and (min-width: 768px) {
        .page-home-container {
            top: 12%;
        }
        .recent-visit-form {
            top: 12%;
        }
    }
    @media screen and (min-width: 992px) {
        .page-home-container {
            top: 12%;
        }
        .recent-visit-form {
            top: 12%;
        }
    }
    @media screen and (min-width: 1200px) {
        .page-home-container {
            top: 12%;
        }
        .recent-visit-form {
            top: 12%;
        }
    }
    @media screen and (min-width: 1920px) {
        .page-home-container {
            top: 25%;
        }
        .recent-visit-form {
            top: 25%;
        }
    }
}
.recent-visit-form {
    position: relative;
    width: 900px;
    margin: 0 auto;
}
.quick-access {
  border: none;
  padding: 0;
  margin: 15px 0;
  .quick-access-title {
    font-size: 18px;
    color: #686868;
    width: 100%;
    text-align: center;
  }
  .tag-content {
    margin: 10px 0;
    text-align: center;
    .status-tag {
        background: #E0E0E0;
        color: #686868;
        border: 1px solid #B0B0B0;
        margin: 5px 10px;
        font-size: 13px;
        padding: 5px 10px;
        height: 18px;
        line-height: 18px;
        border-radius: 4px;
        cursor: pointer;
        display: inline-block;
        &:hover {
            background: #f4f7f7;
        }
    }
  }
}
</style>
