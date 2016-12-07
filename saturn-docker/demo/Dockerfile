FROM java:7
MAINTAINER jamin.li@vipshop.com

ARG SATURN_EXECUTOR_DOWNLOAD_URL

RUN wget ${SATURN_EXECUTOR_DOWNLOAD_URL} \
 && unzip ./saturn-executor*.zip \
 && rm -rf ./saturn-executor*.zip \
 && mkdir -p /saturn-executor-master-SNAPSHOT/demo/ \
 && mkdir -p /saturn-executor-master-SNAPSHOT/apps/ \
 && cp /saturn-executor-master-SNAPSHOT/lib/*.jar /saturn-executor-master-SNAPSHOT/apps/

COPY ./InitSaturnJob.class /saturn-executor-master-SNAPSHOT/demo/InitSaturnJob.class
COPY ./demo-java-job.jar /saturn-executor-master-SNAPSHOT/apps/demo-java-job.jar 
COPY ./demo-init.sh /saturn-executor-master-SNAPSHOT/demo-init.sh

RUN chmod a+x /saturn-executor-master-SNAPSHOT/demo-init.sh

ENV TZ "Asia/Shanghai"
ENV LIB_JARS=/saturn-executor-master-SNAPSHOT/lib/*:$CLASSPATH

WORKDIR /saturn-executor-master-SNAPSHOT

CMD ["/saturn-executor-master-SNAPSHOT/demo-init.sh"]