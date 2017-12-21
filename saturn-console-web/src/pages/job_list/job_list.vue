<template>
    <div>
        <Top-bar :domain="domainName" :person="person"></Top-bar>
        <Aside :sidebar-menus="sidebarMenus">
            <router-view></router-view>
        </Aside>
    </div>
</template>

<script>
export default {
  data() {
    return {
      domainUrl: this.$store.getters.domainUrl,
      domainName: this.$route.params.domain,
      sidebarMenus: [
        { index: 'job_overview', title: '作业总览', icon: 'fa fa-bar-chart-o fa-fw', name: 'job_overview', params: { domain: this.$route.params.domain } },
        { index: 'executor_overview', title: 'Executor总览', icon: 'fa fa-area-chart', name: 'executor_overview', params: { domain: this.$route.params.domain } },
      ],
      person: {
        develop: [{ name: '张三', nick: 'san.zhang' }, { name: '李四', nick: 'si.li' }, { name: '王五', nick: 'wu.wang' }],
        operations: [{ name: '哈哈', nick: 'ha.ha' }, { name: '吴六', nick: 'liu.wu' }],
      },
    };
  },
  methods: {
  },
};
</script>
<style lang="sass" scoped>
</style>
