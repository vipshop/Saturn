#!/bin/sh

\cp ../quickstart/demo-java-job.jar ./demo/

#get the latest snapshot version
#VERSION=LATEST
#REPO=snapshots

#get the 3.1.0 release
VERSION=3.1.0
REPO=releases

EXECUTOR_DL_URL="https://oss.sonatype.org/service/local/artifact/maven/content?r=${REPO}&g=com.vip.saturn&a=saturn-executor&v=${VERSION}&e=zip&c=zip"
CONSOLE_DL_URL="https://oss.sonatype.org/service/local/artifact/maven/content?r=${REPO}&g=com.vip.saturn&a=saturn-console&c=exec&v=${VERSION}"

docker build -t saturn/saturn-db ./saturn-db/
docker build --build-arg SATURN_CONSOLE_DOWNLOAD_URL=$CONSOLE_DL_URL -t saturn/saturn-console ./saturn-console/
docker build --build-arg SATURN_EXECUTOR_DOWNLOAD_URL=$EXECUTOR_DL_URL -t saturn/demo-init ./demo/

docker-compose up
