FROM java:8

ARG SATURN_CONSOLE_DOWNLOAD_URL

RUN wget ${SATURN_CONSOLE_DOWNLOAD_URL} -O saturn-console.jar

ENV TZ "Asia/Shanghai"

EXPOSE 9088

CMD ["java", "-DSATURN_CONSOLE_DB_URL=jdbc:mysql://db:3306/saturn_console", "-DSATURN_CONSOLE_DB_USERNAME=root", "-DSATURN_CONSOLE_DB_PASSWORD=defaultpass", "-Dsaturn.stdout=true", "-Dsaturn.embeddedZk=true", "-jar", "saturn-console.jar"]
