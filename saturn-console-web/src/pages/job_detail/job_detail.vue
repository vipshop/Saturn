<template>
    <div>
        <Top-bar :domain="domainName" :person="person"></Top-bar>
        <Aside :sidebar-menus="sidebarMenus">
            <div class="page-header">
                <div class="pull-left page-title">作业 :  {{jobName}}</div>
                <div class="pull-right">
                    <el-button size="small" @click=""><i class="fa fa-play-circle text-danger"></i>启用</el-button>
                    <el-button size="small" @click=""><i class="fa fa-play-circle-o text-danger"></i>立即执行</el-button>
                    <el-button size="small" @click=""><i class="fa fa-stop-circle-o text-danger"></i>立即终止</el-button>
                    <el-button size="small" @click=""><i class="fa fa-trash text-danger"></i>删除</el-button>
                </div>
            </div>
            <router-view></router-view>
        </Aside>
    </div>
</template>

<script>
export default {
  data() {
    return {
      domainName: this.$route.params.domain,
      jobName: this.$route.params.jobName,
      sidebarMenus: [
        { index: 'job_setting', title: '作业设置', icon: 'fa fa-gear', name: 'job_setting', params: { domain: this.$route.params.domain, jobName: this.$route.params.domain } },
        { index: 'job_executor', title: '分片情况', icon: 'fa fa-server', name: 'job_executor', params: { domain: this.$route.params.domain, jobName: this.$route.params.domain } },
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
.page-header {
  padding-bottom: 10px;
  border-bottom: 1px solid #d0d0d0;
  height: 30px;
  .page-title {
    height: 30px;
    line-height: 30px;
    font-weight: bold;
    font-size: 16px;
    color: #777;
  }
  >* {
    display: inline-block;
  }
}
</style>
