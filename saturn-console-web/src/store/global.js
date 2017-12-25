/* eslint-disable no-param-reassign */
import * as types from './types';

export default {
  state: {
    domainInfo: {
      nameAndNamespace: '',
      sysAdmin: '',
      techAdmin: '',
    },
  },

  getters: {
    domainInfo: state => state.domainInfo,
  },

  mutations: {
    [types.SET_DOMAIN_INFO](state, item) {
      state.domainInfo = {
        ...state.domainInfo,
        nameAndNamespace: item.nameAndNamespace,
        sysAdmin: item.sysAdmin,
        techAdmin: item.techAdmin,
      };
    },
  },

  actions: {
    [types.SET_DOMAIN_INFO]({ commit }, domainInfo) {
      commit(types.SET_DOMAIN_INFO, domainInfo);
    },
  },
};
