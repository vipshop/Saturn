<template>
    <el-dialog title="作业批量分组" v-loading="loading" element-loading-text="请稍等···" :visible.sync="isVisible" :before-close="closeDialog">
        <el-form label-width="140px">
            <el-alert
              style="margin-bottom: 10px;"
              title="输入共同分组后回车产生分组标签，最后点击确定按钮保存"
              show-icon
              type="warning">
            </el-alert>
            <input type="text" v-show="false" />
            <el-form-item label="作业名称" prop="jobName">
                <el-col :span="18">
                    <el-tag type="success" class="form-tags" v-for="item in jobGroups" :key="item.jobName">{{item.jobName}}</el-tag>
                </el-col>
            </el-form-item>
            <el-form-item prop="groups" label="共同分组">
                <el-tag
                    :key="group"
                    v-for="group in groups"
                    type="primary"
                    closable
                    :disable-transitions="false"
                    style="margin: 0 3px 3px 0;"
                    @close="handleDeleteGroup(group)">
                    {{group}}
                </el-tag>
                <el-autocomplete
                    v-if="inputGroupVisible"
                    v-model="groupSelected"
                    ref="saveGroupInput"
                    :fetch-suggestions="querySearchGroups"
                    placeholder="请添加或选择分组"
                    @select="handleInputGroup"
                    @keyup.enter.native="handleInputGroup"
                >
                </el-autocomplete>
                <el-button v-else size="small" @click="showInput">+ 分组</el-button>
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
  props: ['jobGroups'],
  data() {
    return {
      isVisible: true,
      loading: false,
      originGroups: [],
      groups: [],
      groupList: [],
      groupSelected: '',
      inputGroupVisible: false,
    };
  },
  computed: {
    domainName() {
      return this.$route.params.domain;
    },
  },
  methods: {
    querySearchGroups(queryString, cb) {
      const groupList = this.groupList;
      const results = queryString ?
      groupList.filter(this.createStateFilter(queryString)) : groupList;
      cb(results);
    },
    createStateFilter(queryString) {
      return state =>
        state.value.indexOf(queryString) >= 0;
    },
    handleDeleteGroup(group) {
      this.groups.splice(this.groups.indexOf(group), 1);
    },
    showInput() {
      this.inputGroupVisible = true;
      this.$nextTick(() => {
        this.$refs.saveGroupInput.$refs.input.focus();
      });
    },
    validateGroup(group) {
      let flag = false;
      const parten = /^[\u4e00-\u9fa5a-zA-Z0-9_.-]+$/;
      if (group.length > 15) {
        this.$message.errorMessage('分组名称不能超过15个字符');
      } else if (!parten.test(group)) {
        this.$message.errorMessage('分组名称不能使用特殊字符');
      } else if (this.groups.includes(group)) {
        this.$message.errorMessage('该分组已被选择！');
      } else {
        flag = true;
      }
      return flag;
    },
    handleInputGroup() {
      const groupSelected = this.groupSelected;
      if (groupSelected) {
        if (this.validateGroup(groupSelected)) {
          this.groups.push(groupSelected);
        }
      }
      this.inputGroupVisible = false;
      this.groupSelected = '';
    },
    handleSubmit() {
      const params = {
        jobNames: this.jobGroups.map(e => e.jobName),
        oldGroupNames: this.originGroups,
        newGroupNames: this.groups,
      };
      this.loading = true;
      this.$http.post(`/console/namespaces/${this.domainName}/jobs/batchSetGroups`, params).then(() => {
        this.$emit('job-group-success');
      })
      .catch(() => { this.$http.buildErrorHandler('批量作业分组请求失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
    closeDialog() {
      this.$emit('close-dialog');
    },
    intersect(arr0, arr1) {
      return arr0.filter(item => arr1.indexOf(item) !== -1);
    },
    intersectAll(arr) {
      const argumentsArr = Array.prototype.slice.apply(arr);
      const resultArr = argumentsArr.reduce((prev, cur) => this.intersect(prev, cur));
      return resultArr;
    },
    getGroupList() {
      return this.$http.get(`/console/namespaces/${this.domainName}/jobs/groups`).then((data) => {
        this.groupList = data.filter(v => v !== '未分组').map((obj) => {
          const rObj = {};
          rObj.value = obj;
          return rObj;
        });
      })
      .catch(() => { this.$http.buildErrorHandler('获取groups失败！'); });
    },
    init() {
      this.loading = true;
      Promise.all([this.getGroupList()]).then(() => {
        const arr = [];
        if (this.jobGroups && this.jobGroups.length > 0) {
          this.jobGroups.forEach((ele) => {
            arr.push(this.$array.strToArray(ele.groups));
            this.originGroups = JSON.parse(JSON.stringify(this.intersectAll(arr)));
            this.groups = JSON.parse(JSON.stringify(this.intersectAll(arr)));
          });
        }
      }).finally(() => {
        this.loading = false;
      });
    },
  },
  created() {
    this.init();
  },
};
</script>
