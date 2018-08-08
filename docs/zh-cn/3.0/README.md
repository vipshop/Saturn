## Saturn

> 一个分布式作业调度平台

## 简介

Saturn (任务调度系统)是唯品会开源的一个分布式任务调度平台，取代传统的Linux Cron/Spring Batch Job的方式，做到全域统一配置，统一监控，任务高可用以及分片并发处理。

Saturn是在当当开源的Elastic Job基础上，结合各方需求和我们的实践见解改良而成。

本文档针适用于3.0版本

## 重要特性

* 支持多种语言作业，语言无关(Java/Go/C++/PHP/Python/Ruby/shell)
* 支持秒级调度
* 支持作业分片并行执行
* 支持依赖作业串行执行
* 支持作业高可用和智能负载均衡
* 支持异常检测和自动失败转移
* 支持异地容灾
* 支持多个集群部署
* 支持跨机房区域部署
* 支持弹性动态扩容
* 支持优先级和权重设置
* 支持docker容器，容器化友好
* 支持cron时间表达式
* 支持多个时间段暂停执行控制
* 支持超时告警和超时强杀控制
* 支持灰度发布
* 支持异常、超时和无法高可用作业监控告警和简易的故障排除
* 支持失败率最高、最活跃和负荷最重的各域各节点TOP10的作业统计
* 经受住唯品会生产800多个节点，每日10亿级别的调度考验

## 开发团队

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


