FROM java:7
MAINTAINER jamin.li@vipshop.com

ARG SATURN_CONSOLE_DOWNLOAD_URL

RUN wget ${SATURN_CONSOLE_DOWNLOAD_URL} \
 && mv saturn-console-*.jar saturn-console.jar \
 && mkdir -p /apps/saturn/config

COPY ./nptree.json /apps/saturn/config/nptree.json 

ENV TZ "Asia/Shanghai"
ENV REG_CENTER_JSON_PATH=/apps/saturn/config/nptree.json

EXPOSE 9088

CMD ["java", "-jar", "saturn-console.jar"]