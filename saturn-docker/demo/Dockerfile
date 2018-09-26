FROM java:8

ARG SATURN_EXECUTOR_DOWNLOAD_URL

RUN wget ${SATURN_EXECUTOR_DOWNLOAD_URL} -O saturn-executor.zip \
 && unzip ./saturn-executor.zip \
 && rm -rf ./saturn-executor.zip \
 && mv /saturn-executor-* /saturn-executor \
 && mkdir -p /saturn-executor/demo/ \
 && mkdir -p /saturn-executor/apps/ \
 && cp /saturn-executor/lib/*.jar /saturn-executor/apps/

COPY ./demo-java-job.jar /saturn-executor/apps/demo-java-job.jar 

ADD ./saturn.sh /saturn-executor/saturn.sh

ENV TZ "Asia/Shanghai"
ENV LIB_JARS=/saturn-executor/lib/*:$CLASSPATH

WORKDIR /saturn-executor

RUN ["chmod", "+x", "/saturn-executor/saturn.sh"]

CMD ["/saturn-executor/saturn.sh"]



