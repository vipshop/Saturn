#!/bin/sh

\cp ../quickstart/demo-java-job.jar ./demo/

sudo docker build --build-arg SATURN_EXECUTOR_DOWNLOAD_URL=https://oss.sonatype.org/content/repositories/snapshots/com/vip/saturn/saturn-executor/master-SNAPSHOT/saturn-executor-master-20161205.010145-6-zip.zip -t saturn/saturn-executor:master-SNAPSHOT ./saturn-executor/
sudo docker build --build-arg SATURN_CONSOLE_DOWNLOAD_URL=https://oss.sonatype.org/content/repositories/snapshots/com/vip/saturn/saturn-console/master-SNAPSHOT/saturn-console-master-20161205.005702-7.jar -t saturn/saturn-console:master-SNAPSHOT ./saturn-console/
sudo docker build --build-arg SATURN_EXECUTOR_DOWNLOAD_URL=https://oss.sonatype.org/content/repositories/snapshots/com/vip/saturn/saturn-executor/master-SNAPSHOT/saturn-executor-master-20161205.010145-6-zip.zip -t saturn/demo-init ./demo/

sudo docker-compose up