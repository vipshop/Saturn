import Store from '../store';

export default {
  hasPerm(permission, namespace) {
    let flag = false;
    const isUseAuth = Store.getters.isUseAuth;
    const myPermissions = Store.getters.userAuthority;
    if (isUseAuth) {
      if (myPermissions.role === 'super') {
        flag = myPermissions.authority.indexOf(permission) > -1;
      } else if (namespace) {
        if (myPermissions.authority) {
          const arr = [];
          myPermissions.authority.forEach((ele) => {
            if (ele.namespace === namespace) {
              ele.permissionList.forEach((ele2) => {
                arr.push(ele2);
              });
            }
          });
          const permissionArr = Array.from(new Set(arr));
          if (permissionArr) {
            flag = permissionArr.indexOf(permission) > -1;
          }
        }
      }
    } else {
      flag = true;
    }
    return flag;
  },
};
