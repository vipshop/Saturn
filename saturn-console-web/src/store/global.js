/* eslint-disable no-param-reassign */
import * as types from './types';

export default {
  state: {
    domainUrl: '',
  },

  getters: {
    domainUrl: state => state.domainUrl,
  },

  mutations: {
    [types.SET_DOMAIN_URL](state, items) {
      state.domainUrl = items;
    },
  },

  actions: {
    [types.SET_DOMAIN_URL]({ commit }, domainUrl) {
      commit(types.SET_DOMAIN_URL, domainUrl);
    },
  },
};
