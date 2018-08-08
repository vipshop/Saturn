# FAQ

### **Q: Executor启动失败怎么办？**

**A:** executor启动失败，首先看提示是否参数问题，是否端口重用，如果都不是，根据控制台提示的路径，查看saturn-nohup.out的日志。

**日志报“Fail to discover zk connection string! Please make sure that you have added your namespace on Saturn Console”的情况：**

请检查Console是否已经注册该域。

**日志报“Time different between job sever and register center exceed [60] seconds”；**

这是因为机器时间和zookeeper时间差过大，需要时钟同步。

**日志报“The executor name(xx) is running, cannot running the instance twice”；**

这种一般是因为没有使用脚本stop，而是直接kill -9杀掉进程后立即启动，主要等待20s再启动就ok了。

**日志报“Exception in thread "main" java.lang.UnsupportedClassVersionError: com/vip/saturn/job/executor/Main : Unsupported major.minor version 51.0”：**

这个一般是jdk版本不对(saturn支持jdk版本为1.7和1.8)。

### **Q: 负荷是什么意思？有什么用途?**

**A：**负荷指的是作业消耗资源的轻重程度，使用它可一定程度上控制作业的分片分配。

举例如下：

A域有两台executor, e1和e2。

新增加一个作业job1，job1设分片个数为2，负荷设为1。运行的时候，e1和e2各拿到一个分片，于是e1的负荷为1,e2的负荷为1。

再增加一个作业job2, job2设分片个数为1，负荷为10，运行的时候，e1拿到job2的分片，于是e1的负荷变为1 + 10 = 11, e2的负荷不变。

再增加一个作业 job3,设分片个数为1，负荷为5，运行的时候，saturn判断出来e2的负荷最小，于是把job3的分片分配给它。于是e2的负荷变为1+5 = 6，e1的负荷为11不变。

### **Q：本地模式有何用途？**

**A：**本地模式的作业有多少个executor就有多少个分片，每个executor有且只有一个分片。在配置的时候，如果勾先了本地模式，那么分片个数就无效了，Saturn会判断有多少个executor，然后为它们各自生成一个分片。本地模式的作业在executor增加时自动运行，在executor退出的时候，将不会failover。常见的场景如定时清理本地机器的日志文件，这样当机器增加或减少的时候，无需去更改作业的配置。

### Q: 日志文件没有生成怎么办？

A: 查查Executor启动日志（如果是3.0.0之前的版本，检查saturn-executor.log，3.0.0之后的版本检查saturn-nohup.out），看看日志尤其是logback配置加载部分说了啥。

如果是Executor的日志都出不来，那有可能你用的是嵌入式启动的方式，这个需要自行debug，我们测试过的方式是standalone。

如果是Executor日志有但是业务日志出不来，可能的原因包括但不局限于：

- 作业代码所依赖的logback/log4j/slf4j打包有问题，例如漏了，版本冲突等；
- 日志配置有问题；
- 同一个lib下面既有log4j又有logback的实现；
- more