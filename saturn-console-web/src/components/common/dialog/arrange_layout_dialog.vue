<template>
    <el-dialog title="作业依赖图" width="70%" :visible.sync="isVisible" :before-close="closeDialog" v-loading="loading" element-loading-text="请稍等···">
        <div>
            <el-row>
                <el-col :span="24">
                    <div>
                        <Relate id="arrangeLayout" :option-info="optionInfo" @job-redirect="jobRedirect"></Relate>
                    </div>
                </el-col>
            </el-row>
        </div>
        <div slot="footer" class="dialog-footer">
            <el-button @click="closeDialog()">取消</el-button>
        </div>
    </el-dialog>
</template>

<script>
export default {
  props: ['arrangeLayoutInfo'],
  data() {
    return {
      isVisible: true,
      loading: false,
      initX: 0,
      initY: 300,
    };
  },
  methods: {
    closeDialog() {
      this.$emit('close-dialog');
    },
    getRelateDataXY(relatesData) {
      let dataX = this.initX;
      let dataY = this.initY;
      const resultData = [];
      relatesData.forEach((ele) => {
        dataX += 50;
        if (ele.length > 1) {
          let count = 0;
          if (ele.length % 2 === 0) {
            dataY = this.initY - 25;
          }
          ele.forEach((ele2, index2) => {
            if (index2 > 0) {
              count += 1;
              if (index2 % 2 === 1) {
                dataY += count * 50;
              } else {
                dataY -= count * 50;
              }
            }
            this.$set(ele2, 'x', dataX);
            this.$set(ele2, 'y', dataY);
            resultData.push(ele2);
          });
        } else {
          this.$set(ele[0], 'x', dataX);
          this.$set(ele[0], 'y', this.initY);
          resultData.push(ele[0]);
        }
      });
      return resultData;
    },
    jobRedirect(jobName) {
      this.$emit('job-redirect', jobName);
    },
  },
  computed: {
    optionInfo() {
      const resultInfo = {
        links: [],
        data: [],
      };
      resultInfo.links = this.arrangeLayoutInfo.paths.map((item) => {
        const lineParams = {
          normal: {
            curveness: 0.2,
          },
        };
        const rObj = {};
        rObj.source = item.source;
        rObj.target = item.target;
        rObj.lineStyle = item.direct ? {} : lineParams;
        return rObj;
      });
      const relateDatas = [];
      this.arrangeLayoutInfo.levels.forEach((ele) => {
        const levelItem = ele.map((obj) => {
          const rObj = {};
          rObj.name = obj;
          return rObj;
        });
        relateDatas.push(levelItem);
      });
      resultInfo.data = this.getRelateDataXY(relateDatas);
      return resultInfo;
    },
  },
};
</script>
