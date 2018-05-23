<template>
    <el-dialog :title="userInfoTitle" width="35%" :visible.sync="isVisible" :before-close="closeDialog" v-loading="loading" element-loading-text="请稍等···">
        <el-form :model="userInfo" :rules="rules" ref="userInfo" label-width="100px">
            <el-form-item label="用户名" prop="userName">
                <el-col :span="20">
                    <el-input v-model="userInfo.userName" :disabled="!isEditable"></el-input>
                </el-col>
            </el-form-item>
            <el-form-item label="权限集合" prop="roleKey">
                <el-col :span="20">
                    <el-select v-model="userInfo.roleKey" style="width: 100%">
                        <el-option v-for="item in roles" :label="item.roleName" :value="item.roleKey" :key="item.roleKey"></el-option>
                    </el-select>
                </el-col>
            </el-form-item>
            <el-form-item label="所属域" prop="namespace" v-if="isShowNamespace" required>
                <el-col :span="20">
                    <el-autocomplete
                      v-model="userInfo.namespace"
                      :fetch-suggestions="querySearchAsync"
                      placeholder="请输入域名"
                      style="width: 100%">
                    </el-autocomplete>
                </el-col>
            </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
            <el-button @click="closeDialog()">取消</el-button>
            <el-button type="primary" @click="handleSubmit()">确定</el-button>
        </div>
    </el-dialog>
</template>

<script>
export default {
  props: ['userInfo', 'userInfoTitle', 'userInfoOperate', 'roles'],
  data() {
    return {
      isVisible: true,
      loading: false,
      rules: {
        userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        roleKey: [{ required: true, message: '请选择用户角色', trigger: 'change' }],
        namespace: [{ validator: this.$validate.validateContainDomain, trigger: 'blur' }],
      },
      allDomains: this.$store.getters.allDomains,
    };
  },
  methods: {
    querySearchAsync(queryString, cb) {
      const domains = this.allDomains;
      const results = queryString ? domains.filter(this.createStateFilter(queryString)) : domains;
      cb(results);
    },
    createStateFilter(queryString) {
      return state =>
        state.value.toLowerCase().indexOf(queryString.toLowerCase()) >= 0;
    },
    handleSubmit() {
      this.$refs.userInfo.validate((valid) => {
        if (valid) {
          if (this.userInfoOperate === 'add') {
            this.userInfoRequest('/console/authorizationManage/addUserRoles');
          } else if (this.userInfoOperate === 'edit') {
            this.userInfoRequest('/console/authorizationManage/updateUserRole');
          }
        }
      });
    },
    userInfoRequest(url) {
      if (this.userInfo.roleKey === 'namespace_developer') {
        this.$set(this.userInfo, 'needApproval', true);
      } else {
        this.$set(this.userInfo, 'needApproval', false);
        if (!this.isShowNamespace) {
          this.$set(this.userInfo, 'namespace', '');
        }
      }
      this.loading = true;
      this.$http.post(url, this.userInfo).then(() => {
        this.$emit('user-info-success');
      })
      .catch(() => { this.$http.buildErrorHandler(`${url}请求失败！`); })
      .finally(() => {
        this.loading = false;
      });
    },
    closeDialog() {
      this.$emit('close-dialog');
    },
  },
  computed: {
    isEditable() {
      return this.userInfoOperate !== 'edit';
    },
    isShowNamespace() {
      let flag = false;
      this.roles.some((ele) => {
        if (this.userInfo.roleKey === ele.roleKey) {
          flag = ele.isRelatingToNamespace;
          return true;
        }
        return false;
      });
      return flag;
    },
  },
};
</script>
