# 编译说明
如果你需要对Saturn进行改动并重新编译和构建，请参考以下说明，可以帮助简化相关问题处理，更顺利的完成构建。

温馨提示：执行所有步骤前，请确认自己的网络环境，最好是能够顺利访问所有依赖资源的网址。
以下步骤以MAC OS为例，其他操作系统请参考此过程进行灵活处理。

### 1、依赖安装
Saturn编译过程依赖jdk、nodejs、python（python2）
#### 1.1、安装JDK
请安装JDK1.7或者1.8，步骤略过
#### 1.2、安装nodejs
请安装nodejs v8.17.0
```Shell
# layouts.download.codeBox.installsNvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
# layouts.download.codeBox.downloadAndInstallNodejsRestartTerminal
nvm install 8
# layouts.download.codeBox.verifiesRightNodejsVersion
node -v # layouts.download.codeBox.shouldPrint
# layouts.download.codeBox.verifiesRightNpmVersion
npm -v # layouts.download.codeBox.shouldPrint
```
#### 1.3、安装python
Saturn当前版本依赖古老的python2，请安装2.7.18
下载地址：https://www.python.org/downloads/release/python-2718/

#### 1.4、检查各个依赖版本
```Shell
#java -version
java version "1.8.0_281"
Java(TM) SE Runtime Environment (build 1.8.0_281-b09)
Java HotSpot(TM) 64-Bit Server VM (build 25.281-b09, mixed mode)
#node -v
v8.17.0
#python --version
Python 2.7.18
```
### 2、构建顺序
Saturn的项目构建依赖一个插件，需要先本地构建和安装下这个插件
```Shell
cd saturn-plugin
mvn install
```
后面就可以整个项目构建了

```Shell
mvn clean
mvn package
```
如果报错的话，大概率是以下几个问题，请检查确认。
1、node版本是否安装正确
2、python版本是否安装正确
3、网络环境是否有问题
