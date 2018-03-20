import Store from '../store';

export default {
  hasPerm(permission, namespace) {
    let flag = false;
    const isUseAuth = Store.getters.isUseAuth;
    const myPermissions = Store.getters.userAuthority;
    if (isUseAuth) {
      if (myPermissions.role === 'super') {
        flag = myPermissions.authority.indexOf(permission) > -1;
      }
      if (namespace) {
        if (myPermissions.authority) {
          let arr = [];
          const hasNamespace = myPermissions.authority.some((ele) => {
            if (ele.namespace === namespace) {
              arr = ele.permissionList;
              return true;
            }
            return false;
          });
          if (hasNamespace) {
            flag = arr.indexOf(permission) > -1;
          }
        }
      }
    } else {
      flag = true;
    }
    return flag;
  },
};
