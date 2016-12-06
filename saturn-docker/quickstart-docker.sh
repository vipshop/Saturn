#!/bin/sh

\cp ../quickstart/demo-java-job.jar ./demo/

docker build -t saturn/saturn-executor:master-SNAPSHOT ./saturn-executor/
docker build -t saturn/saturn-console:master-SNAPSHOT ./saturn-console/
docker build -t saturn/demo-init ./demo/

docker-compose up