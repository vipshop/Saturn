# Executor运维

Saturn 3.0.0致力于让用户做到自运维：一方面为用户trouble shooting提供便利，另外一方面希望作为一站式平台去管理executor。

下面讲讲Saturn自运维三大利器：一键摘流量，一键dump以及Executor重启。

## 1 一键摘流量

当executor上的某个作业遇到问题，而且判断不能恢复（例如作业线程处于socket.read，这种情况下是无法通过强杀终止的），我们可能考虑要重启executor。

直接重启executor比较粗暴，更优雅的做法是先把executor上的其他作业转移到其他executor再进行重启，这就是我们说的流量摘取。

操作很简单，进入“Executor总览页面”找到特定的executor，然后点击摘取流量按钮。如下图所示。

![extract_traffic](_media/extract_traffic.jpg)

要注意的是，作业不会因为摘取了流量而立即终止，只有在作业本次结束后的下一轮执行时才会选择别的executor。因此现在的问题是如何判断是否还有作业在上面跑。我们只要在作业总览页面按照作业状态进行过滤，查看所有“运行中”的作业是否跑在目标机器即可。当我们判断所有作业（分片）都跑到其他executor之后，我们就可以择机进行重启。

对于物理机，当机器重启后，需要点击**恢复流量**按钮进行恢复。而容器，由于重启会启动新的容器，因此不需要进行恢复。

![recover_traffic](_media/recover_traffic.jpg)

生产环境中，需要在变更管理菜单中申请“Executor监控“场景来获得此操作权限。

## 2 一键Dump

当我们想知道为什么作业跑这么久，或者作业为什么hang住，我们的第一反应往往是想针对Executor这个Java进程做jstack，以及看看对应的gc log，"一键dump"就是提供这个功能的利器。

对于executor版本大于3.0.0（包括3.0.0-Mx），我们也会把jstack和gc log备份到executor日志目录下。

具体操作如下所示：

![dump](_media/dump.jpg)

## 3 Executor重启

Executor版本大于（包括）3.0.0，支持在Console重启executor。

![restart](_media/restart.jpg)

