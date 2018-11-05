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
      props: ['id', 'dataOption', 'yAxisTitle'],
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
            title: {
              text: null,
            },
            xAxis: { // X坐标轴   categories类别
              categories: [],
              plotLines: [{  // plotLines：标示线
                value: 30,  // 定义在哪个值上显示标示线，这里是在x轴上刻度为3的值处垂直化一条线
                width: 2,  // 标示线的宽度，2px
                dashStyle: 'solid',  // 默认值是solid实线，这里定义为虚线
                color: 'red', // 线的颜色，定义为红色
              }],
            },
            yAxis: { // Y坐标轴
              title: {
                text: this.yAxisTitle,
              },
              plotLines: [{  // plotLines：标示线
                value: 2,  // 定义在哪个值上显示标示线，这里是在x轴上刻度为3的值处垂直化一条线
                width: 1,  // 标示线的宽度，2px
                dashStyle: 'solid',  // 默认值，这里定义为实线
                color: '#808080', // 线的颜色，定义为灰色
              }],
            },
            legend: { // 图例
              layout: 'vertical',  // 图例内容布局方式，有水平布局及垂直布局可选，对应的配置值是： “horizontal(水平)”， “vertical(垂直)”
              align: 'right',  // 图例在图表中的对齐方式，有 “left”, "center", "right" 可选
              verticalAlign: 'middle',  // 垂直对齐方式，有 'top'， 'middle' 及 'bottom' 可选
              borderWidth: 2, // 边框宽度
            },
            series: [ // 数据列
              {  // 数据列中的 name 代表数据列的名字，并且会显示在数据提示框（Tooltip）及图例（Legend）中
                name: '历史记录',
                data: [],
              }],
          },
        };
      },
      watch: {
        dataOption: 'buildPage',
      },
      methods: {
        buildPage() {
          this.options.xAxis.categories = this.dataOption.xAxis;
          this.options.series = this.dataOption.yAxis;
          Highcharts.chart(this.id, this.options);
        },
      },
    };
</script>

<style scoped>

</style>
