## Saturn

> 一个分布式作业调度平台

## 简介

Saturn (任务调度系统)是唯品会Venus体系的一个组成部分，负责分布式的定时任务的调度，取代传统的Linux Cron/Spring Batch Job的方式，做到全域统一配置，统一监控，任务高可用以及分片并发处理。Saturn基于当当Elastic Job代码基础上自主研发的任务调度系统。

本文档针对版本3.0。

## 特性

* 支持基于事件和时间触发
* 人工指定资源分配策略+自动平均策略结合
* 任务开发语言不受限制。支持Shell(PHP, Bash, python...)作业以及Java作业
* 支持1机1分片的本地化模式任务调度
* 任务按分片并行执行
* 框架和业务代码隔离，零依赖
* 分片的调度按负荷均衡分布
* 可视化管理
* 支持秒级任务触发
* 可视化监控和报警
* 支持容器化(Docker)部署

## Contributors

* Dylan Xue <dylan_xueke@hotmail.com>
* Chembo Huang <chemboking@qq.com>
* Xiaopeng He <hebelala@qq.com>
* Juanying Yang <531948963@qq.com>
* Jeff Zhu <ooniki@163.com>
* Timmy Hu <33457178@qq.com>
* Jamin Li <jaminlai@163.com>
* Gilbert Guo <360578526@qq.com>
* Ivy Li <lixiaojuan2009@gmail.com>
* Lan Jian <569232646@qq.com>


