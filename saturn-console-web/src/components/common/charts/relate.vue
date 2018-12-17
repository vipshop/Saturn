<template>
    <div class="main">
        <div :id="id" style="width: 100%;height:400px"></div>
    </div>
</template>

<script>
import echarts from 'echarts';

require('echarts/lib/chart/graph');

export default {
  props: ['id', 'optionInfo'],
  data() {
    return {
      myChart: {},
      option: {
        symbolSize: 45,
        tooltip: {
          show: true,
          formatter(params) {
            return params.dataType === 'node' ? params.data.description : params.name;
          },
        },
        legend: {
          show: true,
        },
        animationDurationUpdate: 1500,
        animationEasingUpdate: 'quinticInOut',
        series: [{
          type: 'graph',
          layout: 'none',
          focusNodeAdjacency: true,
          circular: {
            rotateLabel: true,
          },
          animation: true,
          symbolSize: 45, // 节点大小
          roam: true, // 通过鼠标迁移
          draggable: false,
          label: {
            normal: {
              show: true, // 是否显示标签
            },
          },
          edgeSymbol: ['circle', 'arrow'], // 边两端标记，设置为箭头等
          edgeSymbolSize: [4, 10],
          edgeLabel: {
            normal: {
              textStyle: {
                fontSize: 10,
              },
            },
          },
          data: [],
          links: [],
          categories: [],
          lineStyle: {
            normal: {
              opacity: 0.9,
              width: 2,
              curveness: 0,
            },
          },
        }],
      },
    };
  },
  watch: {
    optionInfo: {
      handler() {
        this.drawLine();
      },
      deep: true,
    },
  },
  methods: {
    initInvisibleGraphic() {
      // Add shadow circles (which is not visible) to enable drag.
      this.myChart.setOption({
        graphic: echarts.util.map(this.option.series[0].data, (item, dataIndex) => {
          const tmpPos = this.myChart.convertToPixel({ seriesIndex: 0 }, [item.x, item.y]);
          return {
            type: 'circle',
            id: dataIndex,
            position: tmpPos,
            shape: {
              cx: 0,
              cy: 0,
              r: 20,
            },
            invisible: true,
            draggable: true,
            ondrag: echarts.util.curry(this.onPointDragging(), dataIndex),
            z: 100,
          };
        }),
      });
      window.addEventListener('resize', this.updatePosition);
      this.myChart.on('dataZoom', this.updatePosition);
      // this.myChart.on('graphRoam', this.updatePosition);
    },
    updatePosition() {    // 更新节点定位的函数
      this.myChart.setOption({
        graphic: echarts.util.map(this.option.series[0].data, (item) => {
          const tmpPos = this.myChart.convertToPixel({ seriesIndex: 0 }, [item.x, item.y]);
          return {
            position: tmpPos,
          };
        }),
      });
    },
    onPointDragging() {      // 节点上图层拖拽执行的函数
      const self = this;
      return function getXY(dataIndex) {
        const tmpPos = self.myChart.convertFromPixel({ seriesIndex: 0 }, this.position);
        self.option.series[0].data[dataIndex].x = tmpPos[0];
        self.option.series[0].data[dataIndex].y = tmpPos[1];
        self.myChart.setOption(self.option);
        self.updatePosition();
      };
    },
    resize() {
      window.addEventListener('resize', () => {
        this.myChart.resize();
      });
    },
    handleClick() {
      this.myChart.on('click', (handler) => {
        if (handler.dataType === 'node') {
          this.$emit('job-redirect', handler.data.name);
        }
      });
    },
    drawLine() {
      if (this.optionInfo) {
        this.option.series[0].data = this.optionInfo.data;
        this.option.series[0].links = this.optionInfo.links;
        this.option.series[0].categories = this.optionInfo.categories;
        this.myChart = echarts.init(document.getElementById(this.id));
        this.myChart.setOption(this.option);
        this.resize();
        this.handleClick();
        // this.initInvisibleGraphic();  // 可拖拽方法
      }
    },
  },
  mounted() {
    this.drawLine();
  },
};
</script>

<style>
</style>
