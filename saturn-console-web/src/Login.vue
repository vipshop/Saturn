<template>
    <div class="login-box">
        <div class="login-header">
            用户登录
        </div>
        <div class="login-body">
            <el-row>  
                <el-col>  
                    <el-input size="medium" v-model="loginInfo.username" placeholder="请输入帐号"></el-input>   
                </el-col>  
            </el-row>  
            <el-row>  
                <el-col>  
                    <el-input v-model="loginInfo.password" type="password" placeholder="请输入密码"></el-input>  
                </el-col>  
            </el-row>  
            <el-row>
                <el-col>  
                    <el-button @click="login()" style="width:100%" type="primary" v-loading="loading">登录</el-button>  
                </el-col>  
            </el-row>
        </div>
    </div>
</template>
<script>

export default {
  data() {
    return {
      loading: false,
      loginInfo: {
        username: '',
        password: '',
      },
    };
  },
  methods: {
    login() {
      this.loading = true;
      this.$http.post('/console/authentication/login', this.loginInfo).then(() => {
        this.$router.push({ path: '/' });
      })
      .catch(() => { this.$http.buildErrorHandler('登录失败，请重新登录！'); })
      .finally(() => {
        this.loading = false;
      });
    },
  },
  created() {
  },
};
</script>
<style lang="sass">
.login-box {  
    margin: 15% auto;
    width: 400px;
    border: 1px solid #487bb0;
    border-radius: 6px;
    .login-header {
        color: #487bb0;
        padding: 10px;
        border-bottom: 1px solid #487bb0;
    }
    .login-body {
        padding: 20px 40px;
        .el-row {
            margin-bottom: 20px;
            &:last-child {  
                margin-bottom: 0;  
            } 
        }
    }
} 
</style>
