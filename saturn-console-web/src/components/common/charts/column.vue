<template>
    <div>
        <div :id="id" :options="options" style="width: 100%;height:300px;"></div>
    </div>
</template>

<script>
import Highcharts from 'highcharts';
import Exporting from 'highcharts/modules/exporting';
// import DarkUnica from 'highcharts/themes/dark-unica';

Exporting(Highcharts);
// DarkUnica(Highcharts);

export default {
  props: ['id'],
  data() {
    return {
      options: {
        lang: {
          downloadJPEG: '下载JPEG图片',
          downloadPDF: '下载PDF文件',
          downloadPNG: '下载PNG文件',
          downloadSVG: '下载SVG文件',
          printChart: '打印图表',
          resetZoom: '恢复缩放',
        },
        chart: {
          type: 'column',
          marginTop: 25,
        },
        credits: {
          enabled: false,
        },
        title: {
          text: null,
        },
        xAxis: {
          text: '时间',
          type: 'datetime',
          dateTimeLabelFormats: {
            day: '%Y-%m-%d %H:%M:%S',
          },
        },
        yAxis: {
          title: {
            text: '数量',
          },
        },
        tooltip: {
          pointFormat: '<div>{series.name}：<span style="color:{series.color}">{point.y}</span></div>',
          valueSuffix: '个',
          xDateFormat: '%Y-%m-%d %H:%M:%S',
          shared: true,
          useHTML: true,
        },
        plotOptions: {
          column: {
            pointPadding: 0.2,
            borderWidth: 0,
          },
          series: {
            pointStart: Date.UTC(2012, 0, 1),
            pointInterval: 3600 * 1000,
          },
        },
        series: [{
          name: 'colomn1',
          data: [49.9, 71.5, 106.4, 129.2, 144.0],
        }, {
          name: 'colomn2',
          data: [83.6, 78.8, 98.5, 93.4, 106.0],
        }, {
          name: 'colomn3',
          data: [48.9, 38.8, 39.3, 41.4, 47.0],
        }, {
          name: 'colomn4',
          data: [42.4, 33.2, 34.5, 39.7, 52.6],
        }],
      },
    };
  },
  methods: {
  },
  mounted() {
    Highcharts.chart(this.id, this.options);
  },
};
</script>
