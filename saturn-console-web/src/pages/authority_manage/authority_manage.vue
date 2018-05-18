<template>
    <div v-loading="loading" element-loading-text="请稍等···">
        <div class="margin-20">
            <FilterPageList ref="pageListRef" :data="userAuthorityList" :total="total" :order-by="orderBy" :filters="filters">
                <template slot-scope="scope">
                    <el-form :inline="true" class="table-filter">
                        <input type="text" v-show="false"/>
                        <el-form-item label="">
                            <el-select v-model="filters.roleKey" @change="scope.search">
                                <el-option label="全部角色" value=""></el-option>
                                <el-option v-for="item in roles" :label="item.roleName" :value="item.roleKey" :key="item.roleKey"></el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="">
                            <el-input placeholder="请输入用户名" v-model="filters.userName" @keyup.enter.native="scope.search"></el-input>
                        </el-form-item>
                        <el-form-item label="">
                            <el-autocomplete
                              v-model="filters.namespace"
                              :fetch-suggestions="querySearchAsync"
                              placeholder="请输入域名"
                              @select="handleSelect">
                            </el-autocomplete>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" icon="el-icon-search" @click="scope.search">查询</el-button>
                            <el-button type="primary" icon="el-icon-plus" @click="handleAdd()">添加用户</el-button>
                        </el-form-item>
                    </el-form>
                    <div class="page-table">
                        <div class="page-table-header">
                            <div class="page-table-header-title"><i class="fa fa-list"></i>用户权限列表
                                <el-button type="text" @click="init"><i class="fa fa-refresh"></i></el-button>
                            </div>
                        </div>
                        <el-table stripe border @sort-change="scope.onSortChange" :data="scope.pageData" style="width: 100%">
                            <el-table-column prop="userName" label="用户名"></el-table-column>
                            <el-table-column prop="roleKey" label="角色">
                                <template slot-scope="scope">
                                    {{rolesMaps[scope.row.roleKey] || scope.row.roleKey}}
                                </template>
                            </el-table-column>
                            <el-table-column prop="namespace" label="所属域">
                                <template slot-scope="scope">
                                    {{scope.row.namespace || '-'}}
                                </template>
                            </el-table-column>
                            <el-table-column prop="lastUpdateTime" label="更新时间" width="160px">
                                <template slot-scope="scope">
                                    {{scope.row.lastUpdateTime | formatDate}}
                                </template>
                            </el-table-column>
                            <el-table-column label="操作" width="100px" align="center">
                                <template slot-scope="scope">
                                    <el-tooltip content="编辑" placement="top">
                                        <el-button type="text" icon="el-icon-edit" @click="handleEdit(scope.row)"></el-button>
                                    </el-tooltip>
                                    <el-tooltip content="删除" placement="top">
                                        <el-button type="text" icon="el-icon-delete text-danger" @click="handleDelete(scope.row)"></el-button>
                                    </el-tooltip>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                </template>
            </FilterPageList>
            <div v-if="isUserVisible">
                <user-info-dialog :user-info="userInfo" :user-info-title="userInfoTitle" :user-info-operate="userInfoOperate" :roles="roles" @user-info-success="userInfoSuccess" @close-dialog="closeInfoDialog"></user-info-dialog>
            </div>
        </div>
    </div>
</template>
<script>
import userInfoDialog from './user_info_dialog';

export default {
  data() {
    return {
      loading: false,
      isUserVisible: false,
      userInfo: {},
      userInfoTitle: '',
      userInfoOperate: '',
      userList: [],
      roles: [],
      rolesMaps: {},
      total: 0,
      filters: {
        userName: '',
        roleKey: '',
        namespace: '',
      },
      orderBy: '',
      domains: this.$store.getters.allDomains,
    };
  },
  methods: {
    querySearchAsync(queryString, cb) {
      const domains = this.domains;
      const results = queryString ? domains.filter(this.createStateFilter(queryString)) : domains;
      cb(results);
    },
    createStateFilter(queryString) {
      return state =>
        state.value.toLowerCase().indexOf(queryString.toLowerCase()) >= 0;
    },
    handleSelect() {
      this.$refs.pageListRef.search();
    },
    handleAdd() {
      this.isUserVisible = true;
      this.userInfoTitle = '添加用户';
      this.userInfoOperate = 'add';
      const userAddInfo = {
        userName: '',
        roleKey: '',
        namespace: '',
        needApproval: false,
      };
      this.userInfo = JSON.parse(JSON.stringify(userAddInfo));
    },
    handleEdit(row) {
      this.isUserVisible = true;
      this.userInfoTitle = '编辑用户';
      this.userInfoOperate = 'edit';
      const userEditInfo = {
        preUserName: row.userName,
        preRoleKey: row.roleKey,
        preNamespace: row.namespace,
        userName: row.userName,
        roleKey: row.roleKey,
        namespace: row.namespace,
        needApproval: row.needApproval,
      };
      this.userInfo = JSON.parse(JSON.stringify(userEditInfo));
    },
    userInfoSuccess() {
      this.isUserVisible = false;
      this.init();
      this.$message.successNotify('保存用户权限操作成功');
    },
    closeInfoDialog() {
      this.isUserVisible = false;
    },
    handleDelete(row) {
      this.$message.confirmMessage(`确认删除用户 ${row.userName} 吗?`, () => {
        const params = {
          userName: row.userName,
          roleKey: row.roleKey,
          namespace: row.namespace,
        };
        this.$http.post('/console/authorizationManage/deleteUserRole', params).then(() => {
          this.init();
          this.$message.successNotify('删除用户操作成功');
        })
        .catch(() => { this.$http.buildErrorHandler('删除用户请求失败！'); });
      });
    },
    getRoles() {
      return this.$http.get('/console/authorizationManage/getRoles').then((data) => {
        this.roles = data;
        data.forEach((ele) => {
          this.rolesMaps[ele.roleKey] = ele.roleName;
        });
      })
      .catch(() => { this.$http.buildErrorHandler('获取用户角色请求失败！'); });
    },
    getAllUser() {
      return this.$http.get('/console/authorizationManage/getAllUsers').then((data) => {
        this.userList = data;
      })
      .catch(() => { this.$http.buildErrorHandler('获取用户权限列表请求失败！'); });
    },
    init() {
      this.loading = true;
      Promise.all([this.getAllUser(), this.getRoles()]).then(() => {
        this.loading = false;
      });
    },
  },
  computed: {
    userAuthorityList() {
      const arr = [];
      this.userList.forEach((ele) => {
        if (ele.userRoles) {
          ele.userRoles.forEach((ele2) => {
            arr.push(ele2);
          });
        }
      });
      this.total = arr.length;
      return arr;
    },
  },
  components: {
    'user-info-dialog': userInfoDialog,
  },
  created() {
    this.init();
  },
};
</script>
<style lang="sass" scoped>
</style>
