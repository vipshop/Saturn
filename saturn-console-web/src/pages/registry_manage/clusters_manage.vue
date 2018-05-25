<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="margin-20">
            <FilterPageList :data="zkClusterList" :total="total" :order-by="orderBy" :filters="filters">
                <template slot-scope="scope">
                    <el-form :inline="true" class="table-filter">
                        <input type="text" v-show="false"/>
                        <el-form-item label="">
                            <el-input placeholder="搜索" v-model="filters.zkClusterKey" @keyup.enter.native="scope.search"></el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" icon="el-icon-search" @click="scope.search">查询</el-button>
                            <el-button type="primary" icon="el-icon-plus" @click="handleAdd()" v-if="$common.hasPerm('registryCenter:addZkCluster')">添加集群</el-button>
                        </el-form-item>
                    </el-form>
                    <div class="page-table">
                        <div class="page-table-header">
                            <div class="page-table-header-title"><i class="fa fa-list"></i>ZK集群信息
                                <el-button type="text" @click="getAllClusters"><i class="fa fa-refresh"></i></el-button>
                            </div>
                        </div>
                        <el-table stripe border @sort-change="scope.onSortChange" :data="scope.pageData" style="width: 100%">
                            <el-table-column prop="zkClusterKey" label="ID" sortable></el-table-column>
                            <el-table-column prop="zkAlias" label="名称"></el-table-column>
                            <el-table-column prop="offline" label="状态">
                                <template slot-scope="scope"> 
                                    <el-tag :type="scope.row.offline ? '' : 'success'" close-transition>{{statusMap[scope.row.offline]}}</el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column prop="zkAddr" label="连接串" width="500px" :show-overflow-tooltip="true"></el-table-column>
                            <el-table-column label="操作" width="80px" align="center">
                                <template slot-scope="scope">
                                    <el-tooltip content="编辑" placement="top" v-if="$common.hasPerm('registryCenter:addZkCluster')">
                                        <el-button type="text" icon="el-icon-edit" @click="handleEdit(scope.row)"></el-button>
                                    </el-tooltip>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                </template>
            </FilterPageList>
            <div v-if="isClusterVisible">
                <cluster-info-dialog :cluster-info="clusterInfo" :cluster-info-title="clusterInfoTitle" :cluster-info-operate="clusterInfoOperate" @cluster-info-success="clusterInfoSuccess" @close-dialog="closeInfoDialog"></cluster-info-dialog>
            </div>
        </div>
    </div>
</template>
<script>
import clusterInfoDialog from './cluster_info_dialog';

export default {
  data() {
    return {
      loading: false,
      isClusterVisible: false,
      zkClusterList: [],
      clusterInfo: {},
      clusterInfoTitle: '',
      clusterInfoOperate: '',
      filters: {
        zkClusterKey: '',
      },
      orderBy: 'zkClusterKey',
      total: 0,
      statusMap: {
        true: '离线',
        false: '在线',
      },
    };
  },
  methods: {
    handleAdd() {
      const clusterAddInfo = {
        zkClusterKey: '',
        alias: '',
        connectString: '',
      };
      this.clusterInfoTitle = '添加ZK集群';
      this.clusterInfoOperate = 'add';
      this.clusterInfo = JSON.parse(JSON.stringify(clusterAddInfo));
      this.isClusterVisible = true;
    },
    closeInfoDialog() {
      this.isClusterVisible = false;
    },
    clusterInfoSuccess() {
      this.isClusterVisible = false;
      this.getAllClusters();
      this.$message.successNotify('保存ZK集群操作成功,若列表未更新,请稍后手动刷新页面');
    },
    handleEdit(row) {
      this.loading = true;
      this.$http.get('/console/zkClusters', { zkClusterKey: row.zkClusterKey }).then((data) => {
        const clusterEditInfo = {
          zkClusterKey: data.zkClusterKey,
          alias: data.zkAlias,
          connectString: data.zkAddr,
        };
        this.clusterInfoTitle = '编辑ZK集群';
        this.clusterInfoOperate = 'edit';
        this.clusterInfo = JSON.parse(JSON.stringify(clusterEditInfo));
        this.isClusterVisible = true;
      })
      .catch(() => { this.$http.buildErrorHandler('获取集群信息请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    getAllClusters() {
      this.loading = true;
      this.$http.get('/console/zkClusters').then((data) => {
        this.zkClusterList = data;
        this.total = this.zkClusterList.length;
      })
      .catch(() => { this.$http.buildErrorHandler('获取所有集群请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
  },
  created() {
    this.getAllClusters();
  },
  components: {
    'cluster-info-dialog': clusterInfoDialog,
  },
};
</script>
