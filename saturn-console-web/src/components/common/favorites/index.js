import Favorites from './src/main';

/* eslint-disable no-param-reassign */
Favorites.install = function install(Vue, options) {
  const { key = 'oaName', storage = localStorage } = options || {};
  Vue.directive('favorites', {
    bind(el, binding, vnode) {
      /* eslint-disable no-param-reassign */
      function handler() {
        let path = location.hash.substring(1);
        let title = path.split('?')[0].substring(1);
        let group = '';
        const map = JSON.parse(storage.getItem(key) || '{}');
        const parent = vnode.context;
        const self = vnode.componentInstance;
        if (binding.modifiers.del) {
          const item = map[binding.value];
          delete map[binding.value];
          storage.setItem(key, JSON.stringify(map));
          self.$emit('favorites-del', true, 'del', item);
        } else if (binding.modifiers.add) {
          if (!Object.prototype.hasOwnProperty.call(map, path)) {
            if (typeof binding.value === 'string') {
              title = binding.value;
            } else if (typeof binding.value === 'object') {
              if (binding.value.title) {
                title = binding.value.title;
              }
              if (binding.value.path) {
                path = binding.value.path;
              }
              if (binding.value.group) {
                group = binding.value.group;
              }
            }
            map[path] = { title, path, group, date: new Date() };
            storage.setItem(key, JSON.stringify(map));
            self.$emit('favorites-add', true, 'add', map[path]);
          } else {
            self.$emit('favorites-add', false, 'add', map[path]);
          }
        }
        parent.$set(parent, binding.arg || 'favorites', Object.values(map));
        self.$emit('favorites-show', true, 'show', Object.values(map));
      }
      if (el.addEventListener) {
        el.addEventListener('click', handler, false);
      } else if (el.attachEvent) {
        el.attachEvent('onclick', handler);
      } else {
        el.onclick = handler;
      }
    },
  });
  Vue.prototype.$favorites = function favorites(path, title, group) {
    const map = JSON.parse(storage.getItem(key) || '{}');
    if (title) {
      const index = path || location.hash.substring(1);
      Object.assign(map[index], { title, group });
      storage.setItem(key, JSON.stringify(map));
      return map[index];
    } else if (path) {
      return map[path];
    }
    return Object.values(map);
  };
  Vue.component(Favorites.name, Favorites);
};

export default Favorites;
