#!/bin/sh

\cp ../quickstart/demo-java-job.jar ./demo/

#get the latest snapshot version
#VERSION=LATEST
#REPO=snapshots

#get the 2.0.6 release
VERSION=2.0.7
REPO=releases

EXECUTOR_DL_URL="https://oss.sonatype.org/service/local/artifact/maven/content?r=${REPO}&g=com.vip.saturn&a=saturn-executor&v=${VERSION}&e=zip&c=zip"
CONSOLE_DL_URL="https://oss.sonatype.org/service/local/artifact/maven/content?r=${REPO}&g=com.vip.saturn&a=saturn-console&v=${VERSION}"

docker build -t saturn/saturn-db ./saturn-db/
docker build --build-arg SATURN_EXECUTOR_DOWNLOAD_URL=$EXECUTOR_DL_URL -t saturn/saturn-executor ./saturn-executor/
docker build --build-arg SATURN_CONSOLE_DOWNLOAD_URL=$CONSOLE_DL_URL -t saturn/saturn-console ./saturn-console/
docker build --build-arg SATURN_EXECUTOR_DOWNLOAD_URL=$EXECUTOR_DL_URL -t saturn/demo-init ./demo/

docker-compose up
