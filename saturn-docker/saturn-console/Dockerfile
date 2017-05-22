FROM java:7

ARG SATURN_CONSOLE_DOWNLOAD_URL

RUN wget ${SATURN_CONSOLE_DOWNLOAD_URL} -O saturn-console.jar\
 && mkdir -p /apps/saturn/config

COPY ./nptree.json /apps/saturn/config/nptree.json 

ENV TZ "Asia/Shanghai"
ENV REG_CENTER_JSON_PATH=/apps/saturn/config/nptree.json

EXPOSE 9088

CMD ["java", "-DSATURN_CONSOLE_DB_URL=jdbc:mysql://db:3306/saturn_console", "-DSATURN_CONSOLE_DB_USERNAME=root", "-DSATURN_CONSOLE_DB_PASSWORD=defaultpass", "-jar", "saturn-console.jar"]
