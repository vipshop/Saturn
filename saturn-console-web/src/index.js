import 'babel-polyfill';
import 'url-search-params-polyfill';
import Vue from 'vue';
import ElementUI from 'element-ui';
import 'font-awesome/css/font-awesome.css';
import '@cloudux/noah-theme/lib/index.css';
import './components';
import './components/filter';
import App from './App';
import Store from './store';
import router from './routers';
import Utils from './utils';
import './styles/iconfont/iconfont.css';
import './styles/main.scss';

Vue.use(ElementUI);
Vue.use(Utils);

/* eslint-disable no-new */
/* eslint-disable no-undef */
window.vm = new Vue({
  el: '#app',
  store: Store,
  router,
  render: h => h(App),
});
vm.clearZk = function clear(url) {
  this.$message.confirmMessage('确定清除zk吗?', () => {
    this.$http.post(url, '').then(() => {
      this.$message.successNotify('清除zk操作成功，请稍后刷新页面获取最新数据。');
    })
    .catch(() => { this.$http.buildErrorHandler('清除zk请求失败！'); });
  });
};
