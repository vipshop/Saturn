const config = {};
function History(route) {
  const paths = JSON.parse(localStorage.getItem(config.key) || '[]');
  if (route && route.path && route.name === 'job_overview') {
    if (paths.length === config.max) {
      paths.pop();
    }
    const index = paths.findIndex(item => item.path === route.path);
    if (index > -1) {
      paths.splice(index, 1);
    }
    paths.unshift({
      path: route.path,
      name: route.name,
      params: route.params,
    });
    localStorage.setItem(config.key, JSON.stringify(paths));
  } else {
    return paths;
  }
}
/* eslint-disable no-param-reassign */
History.install = function install(Vue, { key = 'history', max = 11 }) {
  config.key = key;
  config.max = max;
  Vue.mixin({
    beforeRouteEnter(to, from, next) {
      next(() => {
        History(to);
      });
    },
    beforeRouteUpdate(to, from, next) {
      History(to);
      next();
    },
  });
  Vue.history = History;
  Vue.prototype.$history = History;
};
export default History;
