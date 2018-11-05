<template>
    <el-dialog title="批量选择优先Executors" v-loading="loading" element-loading-text="请稍等···" :visible.sync="isVisible" :before-close="closeDialog">
        <el-form label-width="140px">
            <el-form-item label="作业名称" prop="type">
                <el-col :span="18">
                    <el-tag type="success" class="form-tags" v-for="item in jobNamesArray" :key="item">{{item}}</el-tag>
                </el-col>
            </el-form-item>
            <el-form-item label="优先Executors" prop="jobName">
                <el-col :span="18">
                    <el-select size="small" v-model="selectedExecutors" filterable multiple placeholder="请选择" style="width: 100%;">
                        <el-option v-for="item in onlineExecutors" :key="item.value" :label="item.value" :value="item.value">
                            <span style="float: left">{{ item.value }}</span>
                            <span style="float: left" v-if="item.container"> (容器组)</span>
                        </el-option>
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
  props: ['domainName', 'jobNamesArray'],
  data() {
    return {
      isVisible: true,
      loading: false,
      onlineExecutors: [],
      selectedExecutors: [],
    };
  },
  methods: {
    handleSubmit() {
      this.setSelectedExecutor();
      const params = {
        jobNames: this.jobNamesArray.join(','),
        preferList: this.selectedExecutors.join(','),
      };
      this.$http.post(`/console/namespaces/${this.domainName}/jobs/preferExecutors`, params).then(() => {
        this.$emit('batch-priority-success');
      })
      .catch(() => { this.$http.buildErrorHandler('批量设置作业的优先Executors失败！'); });
    },
    closeDialog() {
      this.$emit('close-dialog');
    },
    setSelectedExecutor() {
      if (this.selectedExecutors.length > 0) {
        this.selectedExecutors.forEach((ele, index) => {
          this.onlineExecutors.forEach((ele2) => {
            if (ele === ele2.value && ele2.container) {
              this.selectedExecutors[index] = `@${ele}`;
            }
          });
        });
      }
    },
    getOnlineExecutors() {
      this.loading = true;
      this.$http.get(`/console/namespaces/${this.domainName}/executors`).then((data) => {
        const onlineExecutorsArray = [];
        data.forEach((ele) => {
          if (ele.status === 'ONLINE') {
            onlineExecutorsArray.push(ele);
          }
        });
        const setOnlineExecutors = onlineExecutorsArray.map((obj) => {
          const rObj = {};
          if (obj.container) {
            rObj.container = obj.container;
            rObj.value = obj.groupName;
          } else {
            rObj.container = obj.container;
            rObj.value = obj.executorName;
          }
          return rObj;
        });
        const hash = {};
        this.onlineExecutors = setOnlineExecutors.reduce((item, next) => {
          if (!hash[next.value]) {
            hash[next.value] = true;
            item.push(next);
          }
          return item;
        }, []);
      })
      .catch(() => { this.$http.buildErrorHandler('获取Executors失败！'); })
      .finally(() => {
        this.loading = false;
      });
    },
  },
  created() {
    this.getOnlineExecutors();
  },
};
</script>
