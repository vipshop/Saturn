<template>
    <div>
        <VAside :sidebar-menus="sidebarMenus" headerHeight="50">
            <router-view></router-view>
        </VAside>
    </div>
</template>

<script>
export default {
  data() {
    return {
      sidebarMenus: [
        { index: 'namespace_manage', title: '域管理', icon: 'fa fa-life-ring', name: 'namespace_manage' },
        { index: 'clusters_manage', title: 'ZK集群管理', icon: 'fa fa-sitemap', name: 'clusters_manage' },
      ],
    };
  },
  methods: {
  },
};
</script>
