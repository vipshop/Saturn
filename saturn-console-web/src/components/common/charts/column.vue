<template>
    <div>
        <div :id="id" :options="options" style="width: 100%;height:300px;"></div>
    </div>
</template>

<script>
import Highcharts from 'highcharts';
import Exporting from 'highcharts/modules/exporting';

Exporting(Highcharts);

export default {
  props: ['id', 'optionInfo'],
  data() {
    return {
      options: {
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
          categories: this.optionInfo.xCategories,
          labels: {
            rotation: -20,
          },
        },
        yAxis: {
          min: 0,
          title: {
            text: this.optionInfo.yTitle,
          },
          stackLabels: {
            enabled: true,
            style: {
              fontWeight: 'bold',
              color: (Highcharts.theme && Highcharts.theme.textColor) || 'gray',
            },
          },
        },
        legend: {
          enabled: false,
        },
        tooltip: {
          useHTML: true,
          hideDelay: 1000,
          formatter: this.optionInfo.tooltip,
        },
        plotOptions: {
          column: {
            stacking: 'normal',
            dataLabels: {
              enabled: false,
              color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white',
              style: {
                textShadow: '0 0 3px black',
              },
            },
          },
          series: {
            cursor: 'pointer',
            events: {
              click: (e) => {
                this.$router.push({ name: 'job_overview', params: { domain: e.point.domainName } });
              },
            },
          },
        },
        series: this.optionInfo.seriesData,
      },
    };
  },
  mounted() {
    Highcharts.chart(this.id, this.options);
  },
};
</script>
