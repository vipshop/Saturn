# ![logo](https://vipshop.github.io/Saturn/zh-cn/3.x/_media/saturn-logo-new.png)

[![Build Status](https://secure.travis-ci.org/vipshop/Saturn.png?branch=develop)](https://travis-ci.org/vipshop/Saturn)
[![GitHub release](https://img.shields.io/github/release/vipshop/Saturn.svg)](https://github.com/vipshop/Saturn/releases)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Saturn is a platform created by VIP.com(唯品会) to provide a distributed, fault tolerant and high available job scheduling service.

## Why Saturn?

- Time based and language unrestricted job
- Easy job implmentation and web based management
- Parallel subtask(shard) scheduling
- 1-second-level scheduling supported
- Intelligent load based job allocation
- Fail detection & failover support
- Statistical data visualization
- All-around monitoring and easy troubleshooting
- Multi-active cluster deployment support
- Container friendly
- Stand the test of billion times scheduling per day
- and more

## Quick Start 快速安装

Make sure below stuff already been installed：

- Java 7+
- Maven 3.0.4+
- node.js 8.7.0+
- npm 5.4.2+
- git (any version)

Then checkout the code and start:

```
git clone https://github.com/vipshop/Saturn
git checkout develop
cd saturn-docker
# for linux
chmod +x quickstart.sh
sh quickstart.sh
# for MS Windows
# quickstart.bat
```

Or you can quick start [via docker compose](https://vipshop.github.io/Saturn/#/zh-cn/3.x/quickstart?id=_2-docker%E5%90%AF%E5%8A%A8)

**To notice that the quick start just for demo purpose, for production environement, please follow this [instruction](https://vipshop.github.io/Saturn/#/zh-cn/3.x/saturn-console-deployment).**

## Releases 发布历史

[Release notes](https://github.com/vipshop/Saturn/releases)

*[3.3.1](https://github.com/vipshop/Saturn/releases/tag/v3.3.1) is the latest stable release, or checkout the develop branch to try something new and cool.*

## Documents & Tutorials 文档与教程

Please go to https://vipshop.github.io/Saturn for reading the documents of 3.0.

For the document of 2.x, please come [here](https://vipshop.github.io/Saturn/#/zh-cn/2.x/).

## The team 开发团队

[About us](https://github.com/vipshop/Saturn/wiki/Saturn's-Wow-Team)

## Cases 使用案例

![orgs](https://vipshop.github.io/Saturn/zh-cn/3.x/_media/orgs.jpg)

[Organizations using Saturn](https://github.com/vipshop/Saturn/wiki/Organizations-using-Saturn)

使用Saturn的公司如果方便请在[这里](https://github.com/vipshop/Saturn/issues/506)留下公司+网址，方便我们宣传，感谢

## Getting help, and helping out 社区互助

WeChat Group: Please add group owner `viptech128` (备注"saturn") to join us!

## Special thanks 特别鸣谢

Saturn is originated from Dangdang's [elastic job](https://github.com/dangdangdotcom/elastic-job), we enhance it according to our requirement and understandings. Special thanks to Zhang Liang@dangdang who give us a lot of help and suggestions.
