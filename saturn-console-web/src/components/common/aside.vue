<template>
    <div class="my-sider">
        <el-container>
            <el-aside :width="siderWidth">
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
    props: ['sidebarMenus'],
    data() {
      const activeMenu = this.getActiveMenu();
      return {
        collapse: false,
        activeMenu,
        sidebarHeight: {
          height: document.body.clientHeight - 90,
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
        return `${this.$route.name}`;
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
  };
</script>
<style lang="sass">
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
