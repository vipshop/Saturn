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
    getLoginUser() {
      const storeUserAuthority = {
        username: 'null',
        role: 'guest',
        isUseAuthority: false,
        authority: [],
      };
      this.$store.dispatch('setUserAuthority', storeUserAuthority);
      this.initialized = true;
    },
    init() {
      this.getLoginUser();
    },
  },
  created() {
    this.init();
  },
};
</script>
