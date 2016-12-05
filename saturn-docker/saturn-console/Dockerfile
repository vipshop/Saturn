FROM java:7
MAINTAINER jamin.li@vipshop.com

RUN wget https://oss.sonatype.org/content/repositories/snapshots/com/vip/saturn/saturn-console/master-SNAPSHOT/saturn-console-master-20161205.005702-7.jar \
 && mv saturn-console-*.jar saturn-console.jar \
 && mkdir -p /apps/saturn/config

COPY ./nptree.json /apps/saturn/config/nptree.json 

ENV TZ "Asia/Shanghai"
ENV REG_CENTER_JSON_PATH=/apps/saturn/config/nptree.json

EXPOSE 9088

CMD ["java", "-jar", "saturn-console.jar"]