<template>
    <el-dialog title="添加作业" :visible.sync="isVisible" :before-close="closeDialog">
        <el-form :model="jobInfo" :rules="rules" ref="jobInfo" label-width="120px">
            <el-form-item label="作业类型" prop="type">
                <el-col :span="20">
                    <el-select v-model="jobInfo.type" style="width: 100%">
                        <el-option v-for="item in jobTypes" :label="item.label" :value="item.value" :key="item.value"></el-option>
                    </el-select>
                </el-col>
            </el-form-item>
            <el-form-item label="作业名" prop="jobName">
                <el-col :span="20">
                    <el-input v-model="jobInfo.jobName"></el-input>
                </el-col>
            </el-form-item>
            <el-form-item label="cron表达式" prop="cron">
                <el-col :span="20">
                    <el-input v-model="jobInfo.cron"></el-input>
                </el-col>
                <el-col :span="4">
                    <a>预测</a>
                </el-col>
            </el-form-item>
            <el-form-item label="作业分片总数" prop="jobShardNum">
                <el-col :span="20">
                    <el-input-number v-model="jobInfo.jobShardNum" controls-position="right" :min="1" style="width: 100%;"></el-input-number>
                </el-col>
            </el-form-item>
            <el-form-item label="分片序列号/参数对照表" prop="aaa">
                <el-col :span="20">
                    <el-input type="textarea" v-model="jobInfo.aaa"></el-input>
                </el-col>
            </el-form-item>
            <el-form-item label="描述" prop="description">
                <el-col :span="20">
                    <el-input type="textarea" v-model="jobInfo.description"></el-input>
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
  props: [],
  data() {
    return {
      isVisible: true,
      jobTypes: [{
        value: 'java',
        label: 'Java定时作业',
      }, {
        value: 'shell',
        label: 'Shell定时作业',
      }],
      rules: {
        type: [{ required: true, message: '请选择作业类型', trigger: 'change' }],
        jobName: [{ required: true, message: '作业名不能为空', trigger: 'blur' }],
      },
      jobInfo: {
        type: '',
        jobName: '',
        cron: '',
        jobShardNum: 1,
        aaa: '',
        description: '',
      },
    };
  },
  methods: {
    handleSubmit() {
      this.$refs.jobInfo.validate((valid) => {
        if (valid) {
          console.log('sumbit');
        }
      });
    },
    closeDialog() {
      this.$emit('close-dialog');
    },
  },
};
</script>
