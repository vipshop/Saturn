<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="margin-20">
            <FilterPageList :data="namespaceList" :total="total" :order-by="orderBy" :filters="filters">
                <template slot-scope="scope">
                    <el-form :inline="true" class="table-filter">
                        <el-form-item label="">
                            <el-select v-model="filters.zkClusterKey" @change="scope.search">
                                <el-option label="全部ZK集群" value=""></el-option>
                                <el-option v-for="item in zkClusterKeys" :label="item.zkAlias" :value="item.zkClusterKey" :key="item.zkClusterKey"></el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="">
                            <el-input placeholder="搜索" v-model="filters.namespace" @keyup.enter.native="scope.search"></el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" icon="el-icon-search" @click="scope.search">查询</el-button>
                            <el-button type="primary" icon="el-icon-plus" @click="handleAdd()" v-if="$common.hasPerm('registryCenter:addNamespace')">添加域</el-button>
                            <el-button type="primary" icon="el-icon-refresh" @click="handleRefreshCenter()" v-if="$common.hasPerm('registryCenter:addNamespace')">刷新注册中心</el-button>
                        </el-form-item>
                    </el-form>
                    <div class="page-table">
                        <div class="page-table-header">
                            <div class="page-table-header-title"><i class="fa fa-list"></i>域信息
                                <el-button type="text" @click="getAllNamespace"><i class="fa fa-refresh"></i></el-button>
                            </div>
                            <div class="page-table-header-separator"></div>
                            <div>
                                <el-button @click="batchMigrate()" v-if="$common.hasPerm('registryCenter:batchMoveNamespaces')"><i class="fa fa-play-circle text-btn"></i>批量迁移</el-button>
                                <el-button @click="handleExport()" v-if="$common.hasPerm('registryCenter:exportNamespaces')"><i class="fa fa-arrow-circle-o-up text-btn"></i>导出</el-button>
                            </div>
                        </div>
                        <el-table stripe border ref="multipleTable" @selection-change="handleSelectionChange" @sort-change="scope.onSortChange" :data="scope.pageData" style="width: 100%">
                            <el-table-column type="selection" width="55"></el-table-column>
                            <el-table-column prop="namespace" label="域名" min-width="110px" sortable>
                                <template slot-scope="scope">
                                    <router-link tag="a" :to="{ name: 'job_overview', params: { domain: scope.row.namespace } }">
                                        <el-button type="text">
                                            {{scope.row.namespace}}
                                        </el-button>
                                    </router-link>
                                </template>
                            </el-table-column>
                            <el-table-column prop="name" label="描述"></el-table-column>
                            <el-table-column prop="degree" label="重要等级" sortable>
                                <template slot-scope="scope">
                                    {{$map.degreeMap[scope.row.degree]}}
                                </template>
                            </el-table-column>
                            <el-table-column prop="version" label="Executor版本" sortable></el-table-column>
                            <el-table-column prop="zkAlias" label="ZK集群名称" sortable>
                                <template slot-scope="scope">
                                    <el-tooltip placement="top">
                                        <div slot="content">{{scope.row.zkAddressList}}</div>
                                        <span>{{scope.row.zkAlias}}</span>
                                    </el-tooltip>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                </template>
            </FilterPageList>
            <div v-if="isNamespaceVisible">
                <namespace-info-dialog :namespace-info="namespaceInfo" :zk-cluster-keys="zkClusterKeys" @close-dialog="closeInfoDialog" @namespace-info-success="namespaceInfoSuccess"></namespace-info-dialog>
            </div>
            <div v-if="isBatchMigrateVisible">
                <batch-migrate-dialog :batch-migrate-info="batchMigrateInfo" :zk-cluster-keys="zkClusterKeys" @batch-migrate-complete="batchMigrateComplete" @close-dialog="closeMigrateDialog"></batch-migrate-dialog>
            </div>
            <div v-if="isMigrateStatusVisible">
                <migrate-status-dialog :migrate-status-title="migrateStatusTitle" @close-dialog="closeMigrateStatusDialog"></migrate-status-dialog>
            </div>
        </div>
    </div>
</template>
<script>
import namespaceInfoDialog from './namespace_info_dialog';

export default {
  data() {
    return {
      loading: false,
      isNamespaceVisible: false,
      isBatchMigrateVisible: false,
      isMigrateStatusVisible: false,
      migrateStatusTitle: '',
      namespaceList: [],
      namespaceInfo: {},
      zkClusterKeys: [],
      batchMigrateInfo: {},
      filters: {
        namespace: '',
        zkClusterKey: '',
      },
      orderBy: 'namespace',
      total: 0,
      multipleSelection: [],
    };
  },
  methods: {
    handleExport() {
      this.batchOperation('导出', (data) => {
        window.location.href = `/console/namespaces/export?namespaceList=${data}`;
      });
    },
    handleRefreshCenter() {
      this.loading = true;
      this.$http.post('/console/registryCenter/refresh', '').then(() => {
        this.$message.successNotify('刷新注册中心操作完成');
        this.init();
      })
      .catch(() => { this.$http.buildErrorHandler('刷新注册中心请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    batchMigrate() {
      this.batchOperation('迁移', (arr) => {
        this.isBatchMigrateVisible = true;
        const batchMigrateData = {
          namespacesArray: arr,
          zkClusterKeyNew: '',
        };
        this.batchMigrateInfo = JSON.parse(JSON.stringify(batchMigrateData));
      });
    },
    closeMigrateDialog() {
      this.isBatchMigrateVisible = false;
    },
    batchMigrateComplete(zkClusterKeyNew) {
      this.isBatchMigrateVisible = false;
      this.isMigrateStatusVisible = true;
      this.migrateStatusTitle = `状态（批量域迁移到${zkClusterKeyNew}）`;
    },
    closeMigrateStatusDialog() {
      this.isMigrateStatusVisible = false;
      this.init();
    },
    batchOperation(text, callback) {
      if (this.multipleSelection.length <= 0) {
        this.$message.errorMessage(`请选择要批量${text}的域！！！`);
      } else {
        const selectedNamespaceArray = [];
        this.multipleSelection.forEach((element) => {
          selectedNamespaceArray.push(element.namespace);
        });
        callback(selectedNamespaceArray);
      }
    },
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    handleAdd() {
      this.isNamespaceVisible = true;
      const namespaceAddInfo = {
        zkClusterKey: '',
        namespace: '',
      };
      this.namespaceInfo = JSON.parse(JSON.stringify(namespaceAddInfo));
    },
    closeInfoDialog() {
      this.isNamespaceVisible = false;
    },
    namespaceInfoSuccess() {
      this.isNamespaceVisible = false;
      this.$message.successNotify('添加域操作成功,若列表未更新,请稍后手动刷新页面');
      this.init();
    },
    getAllNamespace() {
      this.$http.get('/console/namespaces/detail').then((data) => {
        this.namespaceList = data;
        this.total = this.namespaceList.length;
      })
      .catch(() => { this.$http.buildErrorHandler('获取所有域请求失败！'); });
    },
    getAllClusters() {
      this.$http.get('/console/zkClusters').then((data) => {
        this.zkClusterKeys = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取所有集群请求失败！'); });
    },
    init() {
      this.loading = true;
      Promise.all([this.getAllNamespace(), this.getAllClusters()]).then(() => {
        this.loading = false;
      });
    },
  },
  created() {
    this.init();
  },
  components: {
    'namespace-info-dialog': namespaceInfoDialog,
  },
};
</script>
