<template>
    <div>
        <template v-if="initialized">
            <Container></Container>
        </template>
        <template v-else>
            <div v-loading.fullscreen="loading" element-loading-text="正在初始化应用，请稍等···"></div>
        </template>
    </div>
</template>
<script>

export default {
  data() {
    return {
      loading: false,
      initialized: false,
    };
  },
  methods: {
    loadAllDomains() {
      return this.$http.get('/console/namespaces').then((data) => {
        const allNamespaces = data.map((obj) => {
          const rObj = {};
          rObj.value = obj;
          return rObj;
        });
        this.$store.dispatch('setAllDomains', allNamespaces);
      })
      .catch(() => { this.$http.buildErrorHandler('获取namespaces失败！'); });
    },
    getLoginUser() {
      const storeUserAuthority = {
        username: 'null',
        role: 'guest',
        authority: [],
      };
      this.$store.dispatch('setUserAuthority', storeUserAuthority);
      this.$store.dispatch('setIsUseAuth', false);
    },
    init() {
      this.loading = true;
      Promise.all([this.getLoginUser(), this.loadAllDomains()]).then(() => {
        this.loading = false;
        this.initialized = true;
      });
    },
  },
  created() {
    this.init();
  },
};
</script>
