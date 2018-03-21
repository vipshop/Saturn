<template>
    <div class="header">
        <div class="header-title">
            <router-link tag="a" :to="{ name: 'job_manage' }">
                <el-button type="text"><span class="header-tip">Saturn Console</span></el-button>
            </router-link>
        </div>
        <div>
            <el-menu :default-active="activeIndex" class="el-menu--dark el-menu--has-container" mode="horizontal" :router="true">
                <template v-for='item in serviceList'>
                  <el-menu-item :index="item.index" :route='item' :key="item.index"><i :class="item.icon"></i>{{item.title}}</el-menu-item>
                </template>
                <div class="pull-right user-dropdown">
                    <el-submenu index="">
                        <template slot="title"><i class="fa fa-user"></i>{{userInfo.username || 'null'}}</template>
                        <!-- <el-menu-item index=""><a><i class="fa fa-sign-out"></i>注销</a></el-menu-item> -->
                        <el-menu-item index=""><a href="https://vipshop.github.io/Saturn/#/" target="_blank"><i class="fa fa-question-circle"></i>帮助</a></el-menu-item>
                        <el-menu-item index=""><a @click="handleVersion"><i class="fa fa-info-circle"></i>关于</a></el-menu-item>
                    </el-submenu>
                </div>
            </el-menu>
        </div>
    </div>
</template>

<script>
export default {
  data() {
    return {
      serviceList: [
        { index: '/job', title: '作业管理', icon: 'fa fa-list-alt', path: this.$routermapper.GetPath('jobManage') },
        { index: '/dashboard', title: 'Dashboard', icon: 'fa fa-pie-chart', path: this.$routermapper.GetPath('dashboardManage') },
        { index: '/alarm', title: '告警中心', icon: 'fa fa-bell', path: this.$routermapper.GetPath('alarmManage') },
        { index: '/registry', title: '注册中心', icon: 'fa fa-server', path: this.$routermapper.GetPath('registryManage') },
        { index: '/system', title: '系统配置', icon: 'fa fa-cog', path: this.$routermapper.GetPath('systemConfigManage') },
      ],
    };
  },
  methods: {
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
  },
};
</script>

<style lang="sass" scoped>
.header {
    width: 100%;
    display: table;
}

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

.user-dropdown {
    padding-right: 20px;
}
</style>
