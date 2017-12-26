export default {
  removeByValue(arr, val) {
    Object.keys(arr).forEach((element, index) => {
      if (arr[index] === val) {
        arr.splice(index, 1);
      }
    });
  },
  isContainValue(arr, val) {
    let flag = false;
    if (arr.length > 0) {
      flag = arr.some((element) => {
        if (element === val) {
          return true;
        }
        return false;
      });
    }
    return flag;
  },
  strToArray(str) {
    if (str !== '') {
      if (str.indexOf(',') > 0) {
        const arr = str.split(',');
        return arr;
      }
      return [str];
    }
    return [];
  },
};
