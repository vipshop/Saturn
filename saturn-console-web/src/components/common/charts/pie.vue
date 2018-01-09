<template>
    <div>
        <div :id="id" :options="options" style="width: 100%;height:300px;"></div>
    </div>
</template>

<script>
import Highcharts from 'highcharts';
import Highcharts3D from 'highcharts/highcharts-3d';
import Exporting from 'highcharts/modules/exporting';

Exporting(Highcharts);
Highcharts3D(Highcharts);

export default {
  props: ['id', 'dataOption'],
  data() {
    return {
      options: {
        lang: {
          downloadJPEG: '下载JPEG图片',
          downloadPDF: '下载PDF文件',
          downloadPNG: '下载PNG文件',
          downloadSVG: '下载SVG文件',
          printChart: '打印图表',
          noData: '暂无数据',
        },
        chart: {
          type: 'pie',
          options3d: {
            enabled: true,
            alpha: 45,
          },
        },
        credits: {
          enabled: false,
        },
        title: {
          text: null,
        },
        plotOptions: {
          pie: {
            innerSize: 100,
            depth: 45,
          },
        },
        series: this.dataOption.seriesData,
      },
    };
  },
  watch: {
    dataOption: 'buildPage',
  },
  methods: {
    buildPage() {
      if (this.dataOption) {
        this.options.series = this.dataOption.seriesData;
        Highcharts.chart(this.id, this.options);
      }
    },
  },
};
</script>
