FROM java:7
MAINTAINER jamin.li@vipshop.com

ARG SATURN_EXECUTOR_DOWNLOAD_URL

RUN apt-get update \
 && apt-get install -y lsof \
 && apt-get clean \
 && wget ${SATURN_EXECUTOR_DOWNLOAD_URL} -O saturn-executor.zip\
 && unzip ./saturn-executor.zip \
 && chmod a+x /saturn-executor-master-SNAPSHOT/bin/saturn-executor.sh \
 && rm -rf ./saturn-executor*.zip \
 && mkdir -p /saturn-executor-master-SNAPSHOT/apps/

ENV TZ "Asia/Shanghai"
ENV VIP_SATURN_ZK_CONNECTION=zookeeper:2181

CMD ["/saturn-executor-master-SNAPSHOT/bin/saturn-executor.sh", "start", "-n", "saturn-it.vip.com", "-r" ,"foreground", "-env", "docker", "-d", "/saturn-executor-master-SNAPSHOT/apps"]
