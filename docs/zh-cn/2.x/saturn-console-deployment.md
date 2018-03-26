# Saturn Console部署

## 1 部署前准备

### 1.1 硬件准备

Linux服务器至少1台，服务器数量视乎计划的Saturn Console的集群及ZK集群的大小。

### 1.2 软件准备

JDK  >= 1.7

ZooKeeper >= 3.4.6 （建议使用ZooKeeper 3.4.6 ([官网下载链接](https://archive.apache.org/dist/zookeeper/zookeeper-3.4.6/))，更高版本未经验证）

## 2 开始部署

### 2.1 安装ZooKeeper

按照官方文档进行安装。

对于生产环境，推荐使用5台服务器组成的集群（1 Leader + 4 Follower）。

推荐的ZooKeeper配置参看**这里**。

### 2.2 安装MySQL

当前Saturn支持2个数据库：MySQL和H2。H2主要用于测试，生产环境推荐使用MySQL。MySQL主要用于存储作业配置及配置历史，系统配置，以及namespace及zk集群信息等等。

关于MySQL的版本，没有任何要求。

安装MySQL，遵循MySQL的官方文档进行这里不作说明。

### 2.3 数据准备

下面操作是针对MySQL，对于H2可以仿照类似的做法。

#### 2.3.1 database创建

数据库的名字可以自行指定。下面是个例子：

```mysql
CREATE DATABASE saturn CHARACTER SET utf8 COLLATE utf8_general_ci;
```

#### 2.3.2 schema创建

从[这里](https://github.com/vipshop/Saturn/blob/develop/saturn-console/src/main/resources/db/mysql/schema.sql)获取最新的**schema.sql**。如果希望获得其他版本的schema，可以在源代码的其他tag上获取。

执行schema.sql。

#### 2.3.3 数据创建

##### 2.3.3.1 注册zk cluster信息

下面的例子是创建一个key位'cluster1'的zk集群。

```mysql
INSERT INTO `zk_cluster_info`(`zk_cluster_key`, `alias`, `connect_string`) VALUES('cluster1', '集群1', 'localhost:2181,localhost:2182');
```

其中：

* **zk_cluster_key**代表集群的ID
* **alias**用于界面展示
* **connection_string**是zk连接串，多个zk server用逗号分隔

##### 2.3.3.2 注册域信息

下面的例子展示如何注册名字为"www.abc.com"的namespace：

```mysql
INSERT INTO `namespace_info`(`namespace`) VALUES('www.abc.com');
```

##### 2.3.3.3 注册域与zk集群的关系

将namespace关联到zk集群。下面以"www.abc.com"关联到cluster1为例子。

```mysql
INSERT INTO `namespace_zkcluster_mapping`(`namespace`, `name`, `zk_cluster_key`) VALUES('www.abc.com', '业务组abc', 'cluster1');
```

##### 2.3.3.4 注册Console与zk集群的关系 （Optional）

设置console与zk集群的关系。只有console（集群）和特定zk集群绑定了，才可以sharding该zk集群上的namespace的作业。

如果只有1个console和1个zk集群，则使用**default:<zk-cluster-name>**即可。

```mysql
INSERT INTO sys_config(property,value) values('CONSOLE_ZK_CLUSTER_MAPPING','default:cluster1');
```

如果你有多个zk集群和console集群，则需要指定console集群和zk集群的关系，下面是例子：

```mysql
INSERT INTO sys_config(property,value) values('CONSOLE_ZK_CLUSTER_MAPPING','console-gd:cluster1;console-bj:cluster2');
```

这个例子表示console集群为console-gd的console管辖cluster1的作业，而console集群console-bj管辖cluster2的作业。关于如何指定console属于哪个console集群，见下面**2.4 安装Console**。

### 2.4 安装Console

#### 2.4.1 下载

从<https://github.com/vipshop/Saturn/releases> 中点击最新版本的“Console Zip File”，下载得到saturn-console-{version}-exec.jar，将之放到合适的目录。

#### 2.4.2 启动Console

```shell
# 可通过参数SATURN_CONSOLE_LOG指定日志路径
nohup java -DSATURN_CONSOLE_DB_URL=jdbc:mysql://localhost:3306/saturn -DSATURN_CONSOLE_DB_USERNAME=root -DSATURN_CONSOLE_DB_PASSWORD=password -jar saturn-console-{version}-exec.jar &
```

访问http://{ip}:9088 即可看到saturn控制台。其中ip指的是console安装的机器的IP。

##### 2.4.2.1 JVM参数推荐

注意，如果是在生产环境启动console，建议增加一些JVM启动参数：

JDK 1.7:

```shell
-Xmx2G -Xms2G -XX:PermSize=256m -XX:MaxPermSize=512m -XX:+UseConcMarkSweepGC -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=75 -XX:+ExplicitGCInvokesConcurrent -Xloggc:${HOME}/gc_zk.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:ErrorFile=${HOME}/hs_err_%p.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${HOME}
```

JDK 1.8:
```shell
-Xmx2G -Xms2G -MetaspaceSize=256m -MaxMetaspaceSize=512m -XX:+UseConcMarkSweepGC -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=75 -XX:+ExplicitGCInvokesConcurrent -Xloggc:${HOME}/gc_zk.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:ErrorFile=${HOME}/hs_err_%p.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${HOME}
```

##### 2.4.2.2 启动参数描述

| 参数名                                      | 是否支持环境变量/JVM参数 | 描述                                | 是否必须 |
| ---------------------------------------- | -------------- | --------------------------------- | ---- |
| VIP_SATURN_CONSOLE_CLUSTER               | 都支持            | Console集群id                       | N    |
| SATURN_CONSOLE_DB_URL                    | 都支持            | DB 连接url                          | Y    |
| SATURN_CONSOLE_DB_USERNAME               | 都支持            | DB用户名                             | Y    |
| SATURN_CONSOLE_DB_PASSWORD               | 都支持            | 密码                                | Y    |
| SATURN_CONSOLE_LOG                       | 都支持            | 日志目录。默认是/apps/logs/saturn_console | N    |
| VIP_SATURN_DASHBOARD_REFRESH_INTERVAL_MINUTE | 都支持            | Dashboard后台刷新频率，单位是分钟。默认值是1。      | N    |
| server.port | JVM参数           | 启动端口，默认9088      | N    |

