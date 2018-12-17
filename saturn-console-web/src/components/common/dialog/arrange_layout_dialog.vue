<template>
    <el-dialog title="作业依赖图" width="70%" custom-class="arrange-layout-content" :visible.sync="isVisible" :before-close="closeDialog" v-loading="loading" element-loading-text="请稍等···">
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
        dataX += 150;
        if (ele.length > 1) {
          let count = 0;
          if (ele.length % 2 === 0) {
            dataY = this.initY - 25;
          } else {
            dataY = this.initY;
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
    isLineCurve(allDatas, sourceData, targetData) {
      let flag = false;
      flag = allDatas.some((ele) => {
        if (sourceData.y === targetData.y) {
          if (ele.y === sourceData.y && ele.x > sourceData.x && ele.x < targetData.x) {
            return true;
          }
        } else if (sourceData.x === targetData.x) {
          if (ele.x === sourceData.x
          && ele.y > Math.min(sourceData.y, targetData.y)
          && ele.y < Math.max(sourceData.y, targetData.y)) {
            return true;
          }
        } else {
          console.log('line');
          if (
            (ele.x > Math.min(sourceData.x, targetData.x)
            && ele.x < Math.max(sourceData.x, targetData.x)) &&
            (ele.y > Math.min(sourceData.y, targetData.y)
            && ele.y < Math.max(sourceData.y, targetData.y))) {
            if (Math.abs((ele.x - sourceData.x) / (ele.y - sourceData.y)) ===
            Math.abs((targetData.x - ele.x) / (targetData.y - ele.y))) {
              return true;
            }
          }
        }
        return false;
      });
      return flag;
    },
  },
  computed: {
    optionInfo() {
      const resultInfo = {
        links: [],
        data: [],
        categories: [],
      };
      const relateDatas = [];
      this.arrangeLayoutInfo.levels.forEach((ele) => {
        const levelItem = ele.map((obj) => {
          const rObj = { ...obj };
          rObj.category = this.$map.jobStatusMap[obj.jobStatus];
          return rObj;
        });
        relateDatas.push(levelItem);
      });
      resultInfo.data = this.getRelateDataXY(relateDatas);
      resultInfo.links = this.arrangeLayoutInfo.paths.map((item) => {
        const lineParams = {
          normal: {
            curveness: 0.2,
          },
        };
        const rObj = {};
        rObj.source = item.source;
        rObj.target = item.target;
        const sourceData = resultInfo.data.find(v => v.name === item.source);
        const targetData = resultInfo.data.find(v => v.name === item.target);
        if (this.isLineCurve(resultInfo.data, sourceData, targetData)) {
          rObj.lineStyle = lineParams;
        }
        return rObj;
      });
      resultInfo.categories = [{
        name: this.$map.jobStatusMap.READY,
        itemStyle: { color: '#487bb0' },
      }, {
        name: this.$map.jobStatusMap.RUNNING,
        itemStyle: { color: '#23ad07' },
      }, {
        name: this.$map.jobStatusMap.STOPPING,
        itemStyle: { color: '#E6A23C' },
      }, {
        name: this.$map.jobStatusMap.STOPPED,
        itemStyle: { color: '#808080' },
      }];
      return resultInfo;
    },
  },
};
</script>
<style lang="sass">
.arrange-layout-content {
    .el-dialog__body {
        padding: 15px;
    }
}
</style>
