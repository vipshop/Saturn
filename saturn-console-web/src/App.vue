<template>
    <div>
        <template v-if="initialized || initComponent === 'Login'">
            <component :is="initComponent" @login-success="loginSuccess"></component>
        </template>
        <template v-else>
            <div v-loading.fullscreen="loading" element-loading-text="正在初始化应用，请稍等···"></div>
        </template>
    </div>
</template>
<script>
import Vue from 'vue';
import Login from './Login';
import Favorites from './components/common/favorites/';

export default {
  data() {
    return {
      loading: false,
      initialized: false,
    };
  },
  methods: {
    loginSuccess() {
      this.init();
    },
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
    getUseAuth() {
      return this.$http.get('/console/authorization/isAuthorizationEnabled').then((data) => {
        this.$store.dispatch('setIsUseAuth', data);
      })
      .catch(() => { this.$http.buildErrorHandler('获取权限开关请求失败！'); });
    },
    getLoginUser() {
      return this.$http.get('/console/authorization/loginUser').then((data) => {
        Vue.use(Favorites, { key: data.userName });
        const storeUserAuthority = {
          username: data.userName,
          authority: [],
        };
        if (data.userRoles.length > 0) {
          data.userRoles.forEach((ele) => {
            const permission = {
              namespace: ele.namespace,
              isRelatingToNamespace: ele.role.isRelatingToNamespace,
              permissionList: [],
            };
            ele.role.rolePermissions.forEach((ele2) => {
              permission.permissionList.push(ele2.permissionKey);
            });
            storeUserAuthority.authority.push(permission);
          });
        }
        this.$store.dispatch('setUserAuthority', storeUserAuthority);
      })
      .catch(() => { this.$http.buildErrorHandler('获取用户请求失败！'); });
    },
    init() {
      this.loading = true;
      Promise.all([this.getLoginUser(), this.getUseAuth(), this.loadAllDomains()]).then(() => {
        this.loading = false;
        this.initialized = true;
      });
    },
  },
  computed: {
    initComponent() {
      let resultComponent = '';
      if (this.$route.path === '/login') {
        resultComponent = 'Login';
      } else {
        resultComponent = 'Container';
      }
      return resultComponent;
    },
  },
  created() {
    this.init();
  },
  components: {
    Login,
  },
};
</script>
