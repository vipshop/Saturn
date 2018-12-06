<template>
    <div>
        <div :id="id" style="width: 100%;height:300px"></div>
    </div>
</template>

<script>
import echarts from 'echarts';

require('echarts/lib/chart/pie');

export default {
  props: ['id', 'optionInfo'],
  data() {
    return {
      myChart: {},
      option: {
        title: {
          show: false,
          text: '',
        },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b} : {c} ({d}%)',
        },
        legend: {
          orient: 'vertical',
          left: 'left',
        },
        series: [],
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
    resize() {
      window.addEventListener('resize', () => {
        this.myChart.resize();
      });
    },
    drawLine() {
      this.option.series = this.optionInfo.seriesData;
      this.myChart = echarts.init(document.getElementById(this.id));
      this.myChart.setOption(this.option);
      this.resize();
    },
  },
  mounted() {
  },
};
</script>

<style>
</style>
