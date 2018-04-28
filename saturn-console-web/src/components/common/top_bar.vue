<template>
    <div>
        <div class="top-bar">
            <div class="top-bar-left">
                <i class="fa fa-home"></i>
                <span>当前域: 
                    <router-link tag="a" :to="{ name: 'job_overview', params: { domain: domain } }">{{this.domain}}</router-link>
                </span>
                <span>所属集群: <router-link tag="a" :to="{ name: 'clusters_manage' }">{{domainInfo.zkAlias}}</router-link></span>
                <el-tooltip placement="right">
                    <div slot="content">
                        运维负责人：<span>{{domainInfo.sysAdmin || '空'}}</span><br/>
                        开发负责人：<span>{{domainInfo.techAdmin || '空'}}</span>
                    </div>
                    <i class="el-icon-warning"></i>
                </el-tooltip>
            </div>
            <div class="top-bar-right">
                <Favorites></Favorites>
            </div>
        </div>
        
    </div>
</template>
<script>
export default {
  props: ['domain', 'domainInfo'],
};
</script>
<style lang="sass">
.top-bar {
    padding: 0 30px;
    height: 40px;
    background: #38465a;
    font-size: 15px;
    line-height: 40px;
    color: #97a8be;
    .top-bar-left {
        float: left;
        >span {
            margin-right: 15px;
            a {
                color: #c2d0d7;
                &:hover {
                    color: #fff;
                }
            }
        }
    }
    .top-bar-right {
        float: right;
        .el-button--text {
            .fa {
                font-size: 16px;
                color: #c2d0d7;
            }
        }
    }
}
</style>
