FROM java:7
MAINTAINER jamin.li@vipshop.com

RUN apt-get update \
 && apt-get install -y lsof \
 && apt-get clean \
 && wget https://oss.sonatype.org/content/repositories/snapshots/com/vip/saturn/saturn-executor/master-SNAPSHOT/saturn-executor-master-20161202.095614-5-zip.zip \
 && unzip ./saturn-executor*.zip \
 && chmod a+x /saturn-executor-master-SNAPSHOT/bin/saturn-executor.sh \
 && rm -rf /saturn-executor*.zip

ENV TZ "Asia/Shanghai"
ENV VIP_SATURN_ZK_CONNECTION=zookeeper:2181

CMD ["/saturn-executor-master-SNAPSHOT/bin/saturn-executor.sh", "start", "-n", "saturn-it.vip.com", "-r" ,"foreground", "-env", "docker"]
