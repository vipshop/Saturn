import Moment from 'moment';
import Store from '../store';

export default {
  hasPerm(permission, namespace) {
    let flag = false;
    const isUseAuth = Store.getters.isUseAuth;
    const myPermissions = Store.getters.userAuthority;
    if (isUseAuth) {
      if (myPermissions.authority) {
        const isAllNamespace = myPermissions.authority.some((ele) => {
          if (ele.namespace === '*') {
            return true;
          }
          return false;
        });
        if (isAllNamespace) {
          const allPermission = this.getAllPermisson(myPermissions.authority, '*');
          if (allPermission) {
            flag = allPermission.indexOf(permission) > -1;
          }
        } else {
          const permissionArr = this.getAllPermisson(myPermissions.authority, namespace);
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
  getAllPermisson(authorityArray, namespace) {
    const allPermissionsArray = [];
    authorityArray.forEach((ele) => {
      if (ele.namespace === namespace) {
        ele.permissionList.forEach((ele2) => {
          allPermissionsArray.push(ele2);
        });
      }
    });
    return Array.from(new Set(allPermissionsArray));
  },
  isContainDomain(value, arr) {
    return arr.some((ele) => {
      if (ele.value === value) {
        return true;
      }
      return false;
    });
  },
  formatDate(time, format) {
    const date = new Date(time);
    const formatTime = Moment(date).format(format);
    return formatTime;
  },
};
