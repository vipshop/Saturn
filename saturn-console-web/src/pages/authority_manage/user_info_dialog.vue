<template>
    <el-dialog :title="userInfoTitle" width="45%" :visible.sync="isVisible" :before-close="closeDialog" v-loading="loading" element-loading-text="请稍等···">
        <el-form :model="userInfo" :rules="rules" ref="userInfo" label-width="100px">
            <el-form-item label="用户名" prop="userNames" v-if="userInfoOperate === 'add'">
                <el-col :span="22">
                    <el-input v-model="userInfo.userNames" type="textarea" placeholder="多个用户名请用英文','分隔" :disabled="!isEditable"></el-input>
                </el-col>
            </el-form-item>
            <el-form-item label="用户名" prop="userName" v-if="userInfoOperate === 'edit'">
                <el-col :span="22">
                    <el-input v-model="userInfo.userName" :disabled="!isEditable"></el-input>
                </el-col>
            </el-form-item>
            <el-form-item label="权限集合" prop="roleKey">
                <el-col :span="22">
                    <el-select v-model="userInfo.roleKey" style="width: 100%">
                        <el-option v-for="item in roles" :label="item.roleName" :value="item.roleKey" :key="item.roleKey"></el-option>
                    </el-select>
                </el-col>
            </el-form-item>
            <el-form-item label="所属域" prop="namespaces" v-if="isShowNamespace && userInfoOperate === 'add'" required>
                <el-col :span="22">
                    <el-select size="mini" v-model="userInfo.namespaces" multiple filterable remote placeholder="请选择域名" style="width: 100%">
                      <el-option v-for="item in allDomains" :key="item.value" :label="item.value" :value="item.value"></el-option>
                    </el-select>
                </el-col>
            </el-form-item>
            <el-form-item label="所属域" prop="namespace" v-if="isShowNamespace && userInfoOperate === 'edit'" required>
                <el-col :span="22">
                    <el-select size="mini" v-model="userInfo.namespace" filterable remote placeholder="请选择域名" style="width: 100%">
                      <el-option v-for="item in allDomains" :key="item.value" :label="item.value" :value="item.value"></el-option>
                    </el-select>
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
        userNames: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        roleKey: [{ required: true, message: '请选择用户角色', trigger: 'change' }],
        namespaces: [{ required: true, message: '请选择域名', trigger: 'change' }],
        namespace: [{ validator: this.$validate.validateContainDomain, trigger: 'blur' }],
      },
      allDomains: this.$store.getters.allDomains,
    };
  },
  methods: {
    handleSubmit() {
      this.$refs.userInfo.validate((valid) => {
        if (valid) {
          if (this.userInfo.roleKey === 'namespace_developer') {
            this.$set(this.userInfo, 'needApproval', true);
          } else {
            this.$set(this.userInfo, 'needApproval', false);
          }
          if (this.userInfoOperate === 'add') {
            const addParams = {
              userNames: this.userInfo.userNames,
              roleKey: this.userInfo.roleKey,
              namespaces: this.isShowNamespace ? this.userInfo.namespaces.join(',') : '',
              needApproval: this.userInfo.needApproval,
            };
            this.userInfoRequest('/console/authorizationManage/addUserRoles', addParams);
          } else if (this.userInfoOperate === 'edit') {
            const editParams = {
              preUserName: this.userInfo.preUserName,
              preRoleKey: this.userInfo.preRoleKey,
              preNamespace: this.userInfo.preNamespace,
              userName: this.userInfo.userName,
              roleKey: this.userInfo.roleKey,
              namespace: this.isShowNamespace ? this.userInfo.namespace : '',
              needApproval: this.userInfo.needApproval,
            };
            this.userInfoRequest('/console/authorizationManage/updateUserRole', editParams);
          }
        }
      });
    },
    userInfoRequest(url, params) {
      this.loading = true;
      this.$http.post(url, params).then(() => {
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
