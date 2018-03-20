import Store from '../store';

export default {
  hasPerm(permission, namespace) {
    let flag = false;
    const myPermissions = Store.getters.userAuthority;
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
    return flag;
  },
};
