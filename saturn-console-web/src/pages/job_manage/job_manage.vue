<template>
    <div>
        <el-main>
            <div class="page-title">
                <h3>应用域</h3>
            </div>
            <div>
                <el-row :gutter="20">
                    <el-col :span="12" :offset="6">
                        <el-autocomplete
                          v-model="domainName"
                          :fetch-suggestions="querySearchAsync"
                          placeholder="请输入域名"
                          @select="handleSelect"
                          style="width: 100%;">
                        <i class="el-icon-search el-input__icon" slot="suffix"></i>
                        </el-autocomplete>
                    </el-col>
                </el-row>
            </div>
        </el-main>
    </div>
</template>

<script>
export default {
  data() {
    return {
      domainName: '',
      domains: [],
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
        state.value.toLowerCase().indexOf(queryString.toLowerCase()) === 0;
    },
    handleSelect(item) {
      this.toJobListPage(item.value);
    },
    toJobListPage(domain) {
      this.$router.push({ name: 'job_overview', params: { domain } });
    },
    loadAllDomains() {
      this.$http.getData('/console/home/namespaces').then((data) => {
        if (data) {
          this.domains = data.map((obj) => {
            const rObj = {};
            rObj.value = obj;
            return rObj;
          });
        }
      });
    },
  },
  created() {
    this.loadAllDomains();
  },
};
</script>
<style lang="sass" scoped>
.page-title {
    font-size: 19px;
    font-weight: normal;
    margin: 10% 0 20px;
    text-align: center;
    color: #159e74;
}
</style>
