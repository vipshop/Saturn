<template>
    <div class="header">
        <div class="header-title">
            <router-link tag="a" :to="{ name: 'job_manage' }">
                <el-button type="text"><span class="header-tip">Saturn Console</span></el-button>
            </router-link>
        </div>
        <div class="header-content">
            <el-menu :default-active="activeIndex" class="el-menu--dark el-menu--has-container" mode="horizontal" :router="true">
                <template v-for='item in serviceList'>
                  <el-menu-item v-if="item.isSuper" :index="item.index" :route='item' :key="item.index"><i :class="item.icon"></i>{{item.title}}</el-menu-item>
                </template>
                <div class="pull-right" style="display: inline-flex">
                    <div class="headerbase-search" v-if="isShowHeaderSearch">
                        <el-autocomplete
                          prefix-icon="el-icon-search"
                          v-model="domainName"
                          :fetch-suggestions="querySearchAsync"
                          placeholder="请输入域名"
                          popper-class="header-autocomplete"
                          @select="handleSelect">
                        </el-autocomplete>
                    </div>
                    <div class="user-dropdown">
                        <el-submenu index="">
                            <template slot="title"><i class="fa fa-user"></i>{{userInfo.username || 'null'}}</template>
                            <el-menu-item index=""><a style="display: block;" @click="handleLogout"><i class="fa fa-sign-out"></i>注销</a></el-menu-item>
                            <el-menu-item index=""><a style="display: block;" href="https://vipshop.github.io/Saturn/#/" target="_blank"><i class="fa fa-question-circle"></i>帮助</a></el-menu-item>
                            <el-menu-item index=""><a style="display: block;" @click="handleVersion"><i class="fa fa-info-circle"></i>关于</a></el-menu-item>
                        </el-submenu>
                    </div>
                </div>
            </el-menu>
        </div>
    </div>
</template>

<script>
export default {
  data() {
    return {
      domains: this.$store.getters.allDomains,
      domainName: '',
      serviceList: [
        { index: '/job', title: '作业管理', icon: 'fa fa-list-alt', path: this.$routermapper.GetPath('jobManage'), isSuper: true },
        { index: '/dashboard', title: 'Dashboard', icon: 'fa fa-pie-chart', path: this.$routermapper.GetPath('dashboardManage'), isSuper: true },
        { index: '/alarm', title: '告警中心', icon: 'fa fa-bell', path: this.$routermapper.GetPath('alarmManage'), isSuper: true },
        { index: '/registry', title: '注册中心', icon: 'fa fa-server', path: this.$routermapper.GetPath('registryManage'), isSuper: true },
        { index: '/system', title: '系统配置', icon: 'fa fa-cog', path: this.$routermapper.GetPath('systemConfigManage'), isSuper: this.$common.hasPerm('systemConfig') },
        { index: '/authority', title: '权限管理', icon: 'fa fa-check-square-o', path: this.$routermapper.GetPath('authorityManage'), isSuper: this.$common.hasPerm('authorizationManage') },
      ],
    };
  },
  methods: {
    handleLogout() {
      this.$http.post('/console/authentication/logout', {}).then(() => {
        this.$router.push({ name: 'login' });
      })
      .catch(() => { this.$http.buildErrorHandler('注销失败！'); });
    },
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
      this.toPage(item.value);
    },
    toPage(domain) {
      this.domainName = '';
      this.$router.push({ name: 'job_overview', params: { domain } });
    },
    handleVersion() {
      this.$http.get('/console/utils/version').then((data) => {
        const versionHtml = `<strong>版本号: ${data}</strong>`;
        this.$message.openMessage(versionHtml);
      })
      .catch(() => { this.$http.buildErrorHandler('获取版本号请求失败！'); });
    },
  },
  computed: {
    activeIndex() {
      return this.$route.path.split('_')[0];
    },
    userInfo() {
      return this.$store.state.global.userAuthority;
    },
    isShowHeaderSearch() {
      let flag = false;
      const pathArr = this.$route.path.split('/');
      if (pathArr[1] === 'job_list' || pathArr[1] === 'job_detail') {
        flag = true;
      }
      return flag;
    },
  },
};
</script>

<style lang="sass">
.header {
    width: 100%;
    display: table;
    .header-title {
        display: table-cell;
        background-color: #14212e; 
        color: #98A5B4;
        height: 50px;
        line-height: 50px;
        font-weight: 700;
        width: 150px;
        text-align: center;
        padding-left: 20px;
        a .el-button {
          height: 50px;
          .header-tip {
            color: #98a5b4;
            font-weight: 700;
            font-size: medium;
          }
        }
    }
    .header-content {
        display: table-cell;
        vertical-align: middle;
    }
}
.headerbase-search {
    display: inline-block;
    position: relative;
    padding-top: 10px;
}
.header-autocomplete {
  width: 300px !important;
  .el-autocomplete-suggestion__wrap {
      margin-top: 0;
      background: #14212e;
      box-shadow: none;
      border: none;
      li {
        color: #c2d0d7;
        &:hover {
          background: #253343;
        }
        &.highlighted {
          background-color: #253343;
        }
      }
  }
  &.el-popper {
      .popper__arrow {
          border-bottom-color: #14212e;
          &::after {
              border-bottom-color: #14212e;
          }
      }
  }
}
</style>
