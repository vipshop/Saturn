import axios from 'axios';
import message from './message';

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
    message.errorMessage(msg);
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
          config.params = dataObj;
          break;
        case 'POST':
          const formData = new URLSearchParams();
          Object.entries(dataObj).forEach((ele) => {
            formData.append(ele[0], ele[1]);
          });
          config.data = formData;
          break;
        default:
          config.data = dataObj;
          break;
      }
      axios.request(config).then((response) => {
        let result;
        if (response.data.success) {
          result = response.data.obj;
        } else {
          message.errorMessage(response.data.message);
        }
        resolve(result);
      })
      .catch((err) => {
        reject(err);
      });
    });
  },
};
