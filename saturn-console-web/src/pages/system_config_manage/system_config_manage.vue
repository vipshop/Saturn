<template>
    <div>
        <Aside :sidebar-menus="sidebarMenus" headerHeight="50">
            <router-view></router-view>
        </Aside>
    </div>
</template>

<script>
export default {
  data() {
    return {
      sidebarMenus: [
        { index: 'console_config', title: 'Console配置', icon: 'fa fa-life-ring', name: 'console_config' },
      ],
    };
  },
  methods: {
  },
};
</script>
