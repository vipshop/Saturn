<template>
    <div class="my-sider">
        <el-container>
            <el-aside :width="siderWidth" style="background-color: #283340;">
                <div class="page-sidebar-container">
                  <div class="page-sidebar" :class="{collapse: collapse}" :style="sidebarHeight">
                    <el-menu mode="vertical" default-active="1" :router="true">
                        <li class="el-menu-item page-sidebar-toggle-btn" @click="collapseChange()" :style="styleObject">
                          <i class="fa fa-angle-left" :class="{'fa-angle-left':!collapse, 'fa-angle-right':collapse}"></i>
                        </li>
                        <el-menu-item v-for="(menu, index) in sidebarMenus" :key="menu.index" class="page-menu-item"
                                      :class="{active: activeMenu === menu.index}"
                                      :style="styleObject"
                                      :index="menu.index" :route="menu">
                            <i :class="menu.icon"></i>
                            <span class="page-menu-item-title">
                                {{menu.title}}
                            </span>
                            <el-tag :type="menu.name === 'unable_failover_jobs' || menu.name === 'change_manage' ? 'warning' : 'danger'" class="warning-tag" v-if="menu.alarmCount && !collapse">{{menu.alarmCount}}</el-tag>
                        </el-menu-item>
                    </el-menu>
                  </div>
                </div>
            </el-aside>
            <el-main>
                <slot></slot>
            </el-main>
        </el-container>
    </div>
</template>

<script>
  export default {
    props: ['sidebarMenus', 'headerHeight'],
    data() {
      const activeMenu = this.getActiveMenu();
      return {
        collapse: false,
        activeMenu,
        sidebarHeight: {
          height: document.body.clientHeight - this.headerHeight,
        },
      };
    },
    methods: {
      collapseChange() {
        this.collapse = !this.collapse;
      },
      init() {
        this.activeMenu = this.getActiveMenu();
      },
      getActiveMenu() {
        const pathArr = this.$route.path.split('/');
        if (pathArr[1] === 'job_list') {
          return pathArr[3];
        } else if (pathArr[1] === 'job_detail') {
          return pathArr[4];
        }
        return this.$route.name;
      },
    },
    watch: {
      $route: 'init',
    },
    computed: {
      styleObject() {
        return {
          paddingLeft: this.collapse ? '15px' : '30px',
        };
      },
      siderWidth() {
        let w;
        if (this.collapse) {
          w = '50px';
        } else {
          w = '190px';
        }
        return w;
      },
    },
    mounted() {
      this.sidebarHeight.height = document.documentElement.clientHeight - this.headerHeight;
      const that = this;
      window.onresize = function temp() {
        that.sidebarHeight.height = document.documentElement.clientHeight - that.headerHeight;
      };
    },
  };
</script>
<style lang="sass">
.warning-tag {
    border-radius: 6px;
    margin-left: 15px;
    min-width: 28px;
    padding: 0 5px;
    text-align: center;
    height: 20px;
    line-height: 20px;
}
.my-sider {
  .el-aside {
    overflow: visible;
  }
}
.page-sidebar-container {
  .page-sidebar {
    width: 190px;
    background-color: #283340;

    .el-menu {
      border-radius: 0;
      border-bottom: none;
      border-right: none;
      padding: 0;

      .fa {
        width: 20px;
      }
      background-color: #283340;
      
      .el-menu-item {
        border: 1px solid #14212e;
        border-bottom: none;
        height: 50px;
        line-height: 50px;
        color: #bac4cd;
        text-align: left;
        padding-left: 30px;
        i.fa {
          color: #617991;
          margin-right: 0;
        }
        &:last-child {
          border-bottom: 1px solid #14212e;
        }
        &.page-sidebar-toggle-btn {
          text-align: center;
          i.fa {
            font-size: 30px;
          }
        }
        &:hover {
          background-color: #334656;
          border-color: #14212e;
        }
        &:focus {
          background-color: #3e596c;
          color: #fff;
          border-left: 2px solid #70d7b9;
          i.fa {
            color: #9db9d2;
          }
        }
        &.active {
          background-color: #3e596c;
          color: #fff;
          border-left: 2px solid #70d7b9;
          i.fa {
            color: #9db9d2;
          }
        }
      }
    }

    &.collapse {
      width: 50px;
      z-index: 2;
      position: relative;

      .el-menu-item.page-menu-item .page-menu-item-title {
        display: none;
      }
      .el-menu-item.page-menu-item:hover {
        width: 170px !important;
        padding-left: 30px !important;
        .page-menu-item-title {
          display: inline;
        }
      }
    }
  }
}
</style>
