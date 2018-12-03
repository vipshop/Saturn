<template>
    <div>
        <div :id="id" style="width: 100%;height:300px"></div>
    </div>
</template>

<script>
import echarts from 'echarts';

require('echarts/lib/chart/bar');

export default {
  props: ['id', 'optionInfo'],
  data() {
    return {
      myChart: {},
      option: {
        color: ['#7CB5EC'],
        tooltip: {},
        grid: {
          top: '40',
          left: '3%',
          right: '3%',
          bottom: '3%',
          containLabel: true,
        },
        xAxis: [
          {
            type: 'category',
            data: [],
            axisLabel: {
              show: true,
              interval: 0,
              rotate: '20',
              formatter(value) {
                const str = value.length < 15 ? value : `${value.substr(0, 13)}...`;
                return str;
              },
            },
          },
        ],
        yAxis: [
          {
            type: 'value',
            name: '',
          },
        ],
        series: [{
          type: 'bar',
          barWidth: '40%',
          itemStyle: {
            normal: {
              label: {
                show: true,
                position: 'top',
                textStyle: {
                  fontWeight: 'bold',
                  color: 'gray',
                  fontSize: 11,
                },
              },
            },
          },
          data: [],
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
    resize() {
      window.addEventListener('resize', () => {
        this.myChart.resize();
      });
    },
    handleClick() {
      this.myChart.on('click', (handler) => {
        if (handler.data.columnType === 'domain') {
          this.$router.push({ name: 'job_overview', params: { domain: handler.data.domainName } });
        } else if (handler.data.columnType === 'job') {
          this.$router.push({ name: 'job_setting', params: { domain: handler.data.domainName, jobName: handler.data.jobName } });
        }
      });
    },
    drawLine() {
      this.option.xAxis[0].data = this.optionInfo.xCategories;
      this.option.yAxis[0].name = this.optionInfo.yTitle;
      this.option.tooltip = this.optionInfo.tooltip;
      this.option.series[0].data = this.optionInfo.seriesData;
      this.myChart = echarts.init(document.getElementById(this.id));
      this.myChart.setOption(this.option);
      this.resize();
      this.handleClick();
    },
  },
  mounted() {
    this.drawLine();
  },
};
</script>

<style>
</style>
