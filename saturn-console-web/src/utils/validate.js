import Store from '../store';

const isIPv4 = (ip) => {
  const re = /^(\d+)\.(\d+)\.(\d+)\.(\d+)$/;
  if (re.test(ip)) {
    if (RegExp.$1 <= 255 && RegExp.$1 >= 0
    && RegExp.$2 <= 255 && RegExp.$2 >= 0
    && RegExp.$3 <= 255 && RegExp.$3 >= 0
    && RegExp.$4 <= 255 && RegExp.$4 >= 0) {
      return true;
    }
    return false;
  }
  return false;
};

const isPort = (str) => {
  const parten = /^(\d)+$/g;
  if (parten.test(str) && Number.parseInt(str, 10) <= 65535 && Number.parseInt(str, 10) >= 0) {
    return true;
  }
  return false;
};

const isCharacterAndNumber = (str, symbolArr) => {
  let flag = true;
  const parten = /[^a-zA-Z0-9-_.]/g;
  if (!symbolArr) {
    if (parten.test(str)) {
      flag = false;
    }
  } else {
    const isError = symbolArr.some((element) => {
      if (parten.test(str) || str.indexOf(element) >= 0) {
        return true;
      }
      return false;
    });
    if (isError) {
      flag = false;
    }
  }
  return flag;
};

const isIpAndPort = (str) => {
  let num = 0;
  if (str) {
    if (str.indexOf(':') <= 0) {
      num = 1;
    } else {
      const arr = str.split(':');
      if (!isIPv4(arr[0])) {
        num = 2;
      }
      if (!isPort(arr[1])) {
        num = 3;
      }
    }
  }
  return num;
};

const isContainEnglishCapital = (str) => {
  const arr = Array.from(str);
  return arr.some((ele) => {
    if (ele.charCodeAt(0) >= 65 && ele.charCodeAt(0) <= 90) {
      return true;
    }
    return false;
  });
};

export default {
  validateIp(rule, value, callback) {
    if (value === '') {
      callback(new Error('请输入ip地址'));
    } else {
      if (!isIPv4(value)) {
        callback(new Error('请输入正确的ip'));
      }
      callback();
    }
  },
  validatePort(rule, value, callback) {
    if (value === '') {
      callback(new Error('请输入端口号'));
    } else {
      if (!isPort(value)) {
        callback(new Error('请输入正确的端口号'));
      }
      callback();
    }
  },
  validateIpPort(rule, value, callback) {
    if (!value) {
      callback(new Error('请输入ip及端口号'));
    } else {
      if (value.indexOf(':') <= 0) {
        callback(new Error('请输入正确的ip端口号，以:连接'));
      } else {
        const arr = value.split(':');
        if (!isIPv4(arr[0])) {
          callback(new Error('请输入正确的ip'));
        }
        if (!isPort(arr[1])) {
          callback(new Error('请输入正确的端口号'));
        }
      }
      callback();
    }
  },
  validateMultipleIpPort(rule, value, callback) {
    if (!value) {
      callback(new Error('请输入ip及端口号'));
    } else {
      if (value.indexOf(',') <= 0) {
        if (isIpAndPort(value) !== 0) {
          callback(new Error('请输入正确的ip端口号，以:连接'));
        }
        callback();
      } else {
        const arr2 = value.split(',');
        const isError = arr2.some((element) => {
          if (isIpAndPort(element) !== 0) {
            return true;
          }
          return false;
        });
        if (isError) {
          callback(new Error('请输入正确的ip端口号，以:连接'));
        }
      }
      callback();
    }
  },
  validateCharacterAndNumber(rule, value, callback) {
    if (value === '') {
      callback(new Error('该内容不能为空'));
    } else {
      if (!isCharacterAndNumber(value)) {
        callback(new Error('不允许中文和特殊字符'));
      }
      callback();
    }
  },
  validateCharacterAndNumberIsExcludePointAndBar(rule, value, callback) {
    if (value === '') {
      callback(new Error('该内容不能为空'));
    } else {
      const arr = ['.', '-'];
      if (!isCharacterAndNumber(value, arr)) {
        callback(new Error('不允许输入中文和特殊字符'));
      }
      if (isContainEnglishCapital(value)) {
        callback(new Error('不允许输入英文大写'));
      }
      callback();
    }
  },
  validateArray(rule, value, callback) {
    if (Array.isArray(value)) {
      if (value.length === 0) {
        callback(new Error('内容为空，请选择'));
      }
      callback();
    }
  },
  validateContainDomain(rule, value, callback) {
    if (!value) {
      callback(new Error('请选择域名'));
    } else {
      const flag = Store.getters.allDomains.some(ele =>
        ele.value === value,
      );
      if (!flag) {
        callback(new Error('该域不存在，请重新选择！'));
      }
      callback();
    }
  },
};
