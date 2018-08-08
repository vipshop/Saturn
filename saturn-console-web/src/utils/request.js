import axios from 'axios';
import message from './message';

let showError = false;

export default {
  get(url, data) {
    return this.request(url, data, 'GET');
  },

  post(url, data) {
    return this.request(url, data, 'POST');
  },

  put(url, data) {
    return this.request(url, data, 'PUT');
  },

  delete(url, data) {
    return this.request(url, data, 'DELETE');
  },

  buildErrorHandler(msg, callback) {
    if (!showError) {
      message.errorMessage(msg);
      showError = false;
    }
    if (callback) {
      callback();
    }
  },

  request(url, dataObj, methodType) {
    return new Promise((resolve, reject) => {
      const config = {
        url,
        method: methodType,
        cache: false,
      };
      /* eslint-disable no-case-declarations */
      switch (methodType) {
        case 'GET':
        case 'DELETE':
          config.params = dataObj;
          break;
        case 'POST':
        case 'PUT':
          const formData = new URLSearchParams(dataObj);
          config.data = formData;
          break;
        default:
          config.data = dataObj;
          break;
      }
      axios.request(config).then((response) => {
        if (response.data.status === 0) {
          showError = false;
          resolve(response.data.obj);
        } else if (response.data.status === 1) {
          if (response.data.message.indexOf('认证失败') > -1) {
            showError = false;
            resolve(response.data);
          } else {
            message.errorMessage(response.data.message);
            showError = true;
            reject();
          }
        } else if (response.data.status === 2) {
          top.location.href = response.data.obj;
        } else {
          message.errorMessage(response.data.message);
          showError = true;
          reject();
        }
      })
      .catch((err) => {
        showError = false;
        reject(err);
      });
    });
  },
};
