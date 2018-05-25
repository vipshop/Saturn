/* eslint-disable no-param-reassign */
import * as types from './types';
import Http from '../utils/request';

export default {
  state: {
    allDomains: [],
    domainInfo: {
      nameAndNamespace: '',
      sysAdmin: '',
      techAdmin: '',
    },
    jobInfo: {},
    jobListInfo: {
      jobList: [],
      totalNumber: 0,
      enabledNumber: 0,
      abnormalNumber: 0,
      total: 0,
    },
    userAuthority: {
      username: '',
      authority: [],
    },
    isUseAuth: true,
  },

  getters: {
    allDomains: state => state.allDomains,
    domainInfo: state => state.domainInfo,
    jobInfo: state => state.jobInfo,
    jobListInfo: state => state.jobListInfo,
    userAuthority: state => state.userAuthority,
    isUseAuth: state => state.isUseAuth,
  },

  mutations: {
    [types.SET_ALL_DOMAINS](state, item) {
      state.allDomains = item;
    },
    [types.SET_DOMAIN_INFO](state, item) {
      state.domainInfo = {
        ...state.domainInfo,
        nameAndNamespace: item.nameAndNamespace,
        sysAdmin: item.sysAdmin,
        techAdmin: item.techAdmin,
      };
    },
    [types.SET_JOB_INFO](state, item) {
      state.jobInfo = {
        ...item,
      };
    },
    [types.SET_JOB_LIST_INFO](state, item) {
      state.jobListInfo = {
        ...state.jobListInfo,
        jobList: item.jobList,
        totalNumber: item.totalNumber,
        enabledNumber: item.enabledNumber,
        abnormalNumber: item.abnormalNumber,
        total: item.total,
      };
      console.log(state.jobListInfo);
    },
    [types.SET_USER_AUTHORITY](state, item) {
      state.userAuthority = {
        ...state.userAuthority,
        username: item.username,
        authority: item.authority,
      };
      console.log(state.userAuthority);
    },
    [types.SET_IS_USE_AUTH](state, item) {
      state.isUseAuth = item;
    },
  },

  actions: {
    [types.SET_ALL_DOMAINS]({ commit }, allDomains) {
      commit(types.SET_ALL_DOMAINS, allDomains);
    },
    [types.SET_DOMAIN_INFO]({ commit }, domainInfo) {
      commit(types.SET_DOMAIN_INFO, domainInfo);
    },
    [types.SET_JOB_INFO]({ commit }, jobInfoParams) {
      return Http.get(`/console/namespaces/${jobInfoParams.domainName}/jobs/${jobInfoParams.jobName}/config`).then((data) => {
        commit(types.SET_JOB_INFO, data);
        return data;
      });
    },
    [types.SET_JOB_LIST_INFO]({ commit }, jobListInfoParams) {
      return Http.get(`/console/namespaces/${jobListInfoParams.domainName}/jobs`).then((data) => {
        const dataResult = {
          jobList: data.jobs,
          totalNumber: data.totalNumber,
          enabledNumber: data.enabledNumber,
          abnormalNumber: data.abnormalNumber,
          total: data.jobs.length,
        };
        commit(types.SET_JOB_LIST_INFO, dataResult);
        return data;
      });
    },
    [types.SET_USER_AUTHORITY]({ commit }) {
      return Http.get('/console/authorization/loginUser').then((data) => {
        const storeUserAuthority = {
          username: data.userName,
          authority: [],
        };
        if (data.userRoles.length > 0) {
          data.userRoles.forEach((ele) => {
            const permission = {
              namespace: ele.namespace,
              isRelatingToNamespace: ele.role.isRelatingToNamespace,
              permissionList: [],
            };
            ele.role.rolePermissions.forEach((ele2) => {
              permission.permissionList.push(ele2.permissionKey);
            });
            storeUserAuthority.authority.push(permission);
          });
        }
        commit(types.SET_USER_AUTHORITY, storeUserAuthority);
        return data;
      });
    },
    [types.SET_IS_USE_AUTH]({ commit }, isUseAuth) {
      commit(types.SET_IS_USE_AUTH, isUseAuth);
    },
  },
};
