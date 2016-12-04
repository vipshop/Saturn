FROM java:7
MAINTAINER jamin.li@vipshop.com

RUN wget https://oss.sonatype.org/content/repositories/snapshots/com/vip/saturn/saturn-console/master-SNAPSHOT/saturn-console-master-20161202.095251-6.jar \
 && mv saturn-console-*.jar saturn-console.jar \
 && mkdir -p /apps/saturn/config \
 && echo "[ { \"nameAndNamespace\":\"/demo/saturn-it.vip.com\", \"zkAddressList\":\"zookeeper:2181\" } ]">/apps/saturn/config/json.txt

ENV TZ "Asia/Shanghai"
ENV REG_CENTER_JSON_PATH=/apps/saturn/config/json.txt

EXPOSE 9088

CMD ["java", "-jar", "saturn-console.jar"]


