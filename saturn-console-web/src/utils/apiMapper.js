const apiMapperlist = [
    { name: 'executor_dump', url: '/console/namespaces/{domainName}/executors/{executorName}/dump', replace: true },
];

export default {
  PushItem(name, value, replace) {
    const items = apiMapperlist.filter(x => x.name === name);
    if (items.length > 0) {
      items.forEach((each) => {
        if (apiMapperlist.indexOf(each) > -1) {
          apiMapperlist.splice(apiMapperlist.indexOf(each), 1);
        }
      });
    }
    apiMapperlist.push({ name, url: value, replace });
  },
  GetUrl(name, dataObj) {
    const items = apiMapperlist.filter(x => x.name === name);
    if (items && items.length > 0) {
      if (items[0].replace) {
        return this.GetUrlReplaced(items[0].url, dataObj);
      }
      return items[0].url;
    }
    throw new Error(`can not find url path for ${name}`);
  },
  GetUrlReplaced(url, dataObj) {
    const regexp = /\{[a-zA-Z0-9_]*\}+/g;
    const urlReplace = url.match(regexp);
    let target = url;
    if (urlReplace && urlReplace.length) {
      // multi
      urlReplace.forEach((value) => {
        const param = value.slice(1, value.length - 1);
        target = target.replace(value, param in dataObj ? dataObj[param] : '');
      });
    } else {
      throw new Error('replace is true, but can not find urlReplace ');
    }
    return target;
  },
};
