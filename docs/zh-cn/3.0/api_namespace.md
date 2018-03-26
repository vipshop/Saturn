## 1 创建Namespace

### 1.1 请求地址

/rest/v1/namespaces

### 1.2 请求方式

POST

### 1.3 参数说明

Body参数：

|    参数名    |   类型   | 是否可选 |       描述       |
| :-------: | :----: | :--: | :------------: |
| namespace | String |  必填  |       域名       |
| zkCluster | String |  必填  | zk cluster key |

示例：
``` json
{
 "namespace":"www.abc.com",
 "zkcluster":"/saturn"
}
```

### 1.4 返回header（只有失败的情况才有）：

application/json;charset=UTF-8

### 1.5 返回结果：

1.5.1 状态码201，创建成功。

1.5.2 状态码400，参数有误。情况包括

1.5.2.1 job已经存在

`{"message":"Invalid request. Job: {parameter} already existed"}`

1.5.3 状态码404，失败。Namespace不存在。

`{"message":"The namespace does not exists"}`

1.5.4 状态码500，失败。内部错误。

`{"message":"Internal server error"}`

## 2 更新Namespace

暂不提供

## 3 删除Namespace

暂不提供

## 4 查询Namespace（by namespace）

### 4.1 请求地址

/rest/v1/namespaces/{namespace}

### 4.2 请求方式

GET

### 4.3 参数说明

|    参数名    |   类型   | 是否可选 |  描述  |
| :-------: | :----: | :--: | :--: |
| namespace | String |  必填  |  域名  |

### 4.4 返回内容格式

application/json;charset=UTF-8

### 4.5 返回结果

4.5.1 状态码200，成功。返回内容为JSON字符串，内容demo如下：
```Json
{
 "namespace":"www.abc.com",
 "zkCluster":"/saturn"
}
```
2.5.2 状态码400，参数错误，必填参数没有填。

`{"message":"Invalid request. Missing parameter: {parameter}"}`

2.5.3 状态码404，Namespace不存在。

`{"message":"The namespace does not exists"}`

2.5.4 状态码500，内部错误。

`{"message":"Internal server error"}`