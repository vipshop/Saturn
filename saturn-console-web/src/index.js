import Vue from 'vue';
import ElementUI from 'element-ui';
import 'font-awesome/css/font-awesome.css';
import '@cloudux/noah-theme/lib/index.css';
import './components';
import './components/filter';
import Store from './store';
import router from './routers';
import Utils from './utils';
// import './element-variables.scss';
import './styles/iconfont/iconfont.css';
import './styles/main.scss';

Vue.use(ElementUI);
Vue.use(Utils);

/* eslint-disable no-new */
/* eslint-disable no-undef */
window.vm = new Vue({
  el: '#app',
  store: Store,
  template: '<div><Container></Container></div>',
  router,
});
vm.cleanShardingCount = function clearZk(value) {
  this.$message.confirmMessage('确定清除zk吗?', () => {
    console.log(value);
  });
};
