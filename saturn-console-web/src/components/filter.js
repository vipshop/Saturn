import Vue from 'vue';
import moment from 'moment';

Vue.filter('formatDate', (time) => {
  let formatTime;
  if (time !== null) {
    const date = new Date(time);
    formatTime = moment(date).format('YYYY-MM-DD HH:mm:ss');
  } else {
    formatTime = '-';
  }
  return formatTime;
});
Vue.filter('formatOnlyDate', (time) => {
  const date = new Date(time);
  return moment(date).format('MM-DD');
});
Vue.filter('formatOnlyTime', (time) => {
  const date = new Date(time);
  return moment(date).format('HH:mm:ss');
});
Vue.filter('duringTime', (time) => {
  const date = moment(new Date(time));
  const s = moment().diff(date, 'seconds');
  const hours = parseInt((s % (60 * 60 * 24)) / (60 * 60), 10);
  const minutes = parseInt((s % (60 * 60)) / 60, 10);
  const seconds = parseInt((s % 60), 10);
  return `${hours}时${minutes}分${seconds}秒`;
});
