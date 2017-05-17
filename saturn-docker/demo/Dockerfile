FROM java:7

ARG SATURN_EXECUTOR_DOWNLOAD_URL

RUN wget ${SATURN_EXECUTOR_DOWNLOAD_URL} -O saturn-executor.zip \
 && unzip ./saturn-executor.zip \
 && rm -rf ./saturn-executor.zip \
 && mv /saturn-executor-* /saturn-executor \
 && mkdir -p /saturn-executor/demo/ \
 && mkdir -p /saturn-executor/apps/ \
 && cp /saturn-executor/lib/*.jar /saturn-executor/apps/

COPY ./InitSaturnJob.class /saturn-executor/demo/InitSaturnJob.class
COPY ./demo-java-job.jar /saturn-executor/apps/demo-java-job.jar 
COPY ./demo-init.sh /saturn-executor/demo-init.sh

RUN chmod a+x /saturn-executor/demo-init.sh

ENV TZ "Asia/Shanghai"
ENV LIB_JARS=/saturn-executor/lib/*:$CLASSPATH

WORKDIR /saturn-executor

CMD ["/saturn-executor/demo-init.sh"]
