#!/bin/sh

\cp ../quickstart/demo-java-job.jar ./demo/

#get the latest snapshot version
VERSION=LATEST
REPO=snapshots

EXECUTOR_DL_URL=https://oss.sonatype.org/service/local/artifact/maven/content?r=${REPO}&g=com.vip.saturn&a=saturn-executor&v=${VERSION}&e=zip&c=zip
CONSOLE_DL_URL=https://oss.sonatype.org/service/local/artifact/maven/content?r=${REPO}&g=com.vip.saturn&a=saturn-console&v=${VERSION}

docker build --build-arg SATURN_EXECUTOR_DOWNLOAD_URL=$EXECUTOR_DL_URL -t saturn/saturn-executor:master-SNAPSHOT ./saturn-executor/
docker build --build-arg SATURN_CONSOLE_DOWNLOAD_URL=$EXECUTOR_DL_URL -t saturn/saturn-console:master-SNAPSHOT ./saturn-console/
docker build --build-arg SATURN_EXECUTOR_DOWNLOAD_URL=$EXECUTOR_DL_URL -t saturn/demo-init ./demo/

docker-compose up
