FROM java:7

ARG SATURN_EXECUTOR_DOWNLOAD_URL

RUN apt-get update \
 && apt-get install -y lsof tofrodos \
 && apt-get clean \
 && wget ${SATURN_EXECUTOR_DOWNLOAD_URL} -O saturn-executor.zip\
 && unzip saturn-executor.zip \
 && mv /saturn-executor-* /saturn-executor \
 && fromdos /saturn-executor/bin/saturn-executor.sh \
 && chmod a+x /saturn-executor/bin/saturn-executor.sh \
 && rm -rf saturn-executor.zip \
 && mkdir -p /saturn-executor/apps/

ENV TZ "Asia/Shanghai"
ENV VIP_SATURN_ZK_CONNECTION=zookeeper:2181

CMD ["/saturn-executor/bin/saturn-executor.sh", "start", "-n", "saturn-it.vip.com", "-r" ,"foreground", "-env", "docker", "-d", "/saturn-executor/apps"]
