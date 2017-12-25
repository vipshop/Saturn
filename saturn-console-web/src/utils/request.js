import axios from 'axios';
import message from './message';

export default {
  getData(url, data) {
    const loading = message.loading();
    return axios.get(url, { params: data })
    .then((response) => {
      loading.close();
      let result;
      if (response.data.success) {
        result = response.data.obj;
      } else {
        message.errorMessage(response.data.message);
      }
      return result;
    })
    .catch(() => {
      message.errorMessage('请求失败');
      loading.close();
    });
  },
  postData(url, data, config) {
    const loading = message.loading();
    return axios.post(url, data, config)
    .then((response) => {
      loading.close();
      let result;
      if (response.data.success) {
        result = response.data.obj;
      } else {
        message.errorMessage(response.data.message);
      }
      return result;
    })
    .catch(() => {
      message.errorMessage('请求失败');
      loading.close();
    });
  },
  deleteData(url, data) {
    const loading = message.loading();
    return axios.delete(url, { params: data })
    .then((response) => {
      loading.close();
      let result;
      if (response.data.success) {
        result = response.data.obj;
      } else {
        message.errorMessage(response.data.message);
      }
      return result;
    })
    .catch(() => {
      message.errorMessage('请求失败');
      loading.close();
    });
  },
  putData(url, data) {
    const loading = message.loading();
    return axios.put(url, data)
    .then((response) => {
      loading.close();
      let result;
      if (response.data.success) {
        result = response.data.obj;
      } else {
        message.errorMessage(response.data.message);
      }
      return result;
    })
    .catch(() => {
      message.errorMessage('请求失败');
      loading.close();
    });
  },
};
