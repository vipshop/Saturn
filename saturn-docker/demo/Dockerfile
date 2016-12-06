FROM java:7
MAINTAINER jamin.li@vipshop.com

RUN wget https://oss.sonatype.org/content/repositories/snapshots/com/vip/saturn/saturn-executor/master-SNAPSHOT/saturn-executor-master-20161205.010145-6-zip.zip \
 && unzip ./saturn-executor*.zip \
 && rm -rf ./saturn-executor*.zip \
 && mkdir -p /saturn-executor-master-SNAPSHOT/demo/ \
 && mkdir -p /saturn-executor-master-SNAPSHOT/apps/ \
 && cp /saturn-executor-master-SNAPSHOT/lib/*.jar /saturn-executor-master-SNAPSHOT/apps/

COPY ./InitSaturnJob.class /saturn-executor-master-SNAPSHOT/demo/InitSaturnJob.class
COPY ./demo-java-job.jar /saturn-executor-master-SNAPSHOT/apps/demo-java-job.jar 
COPY ./demo-init.sh /saturn-executor-master-SNAPSHOT/demo-init.sh

ENV TZ "Asia/Shanghai"
ENV LIB_JARS=/saturn-executor-master-SNAPSHOT/lib/*:$CLASSPATH

WORKDIR /saturn-executor-master-SNAPSHOT

CMD ["/saturn-executor-master-SNAPSHOT/demo-init.sh"]