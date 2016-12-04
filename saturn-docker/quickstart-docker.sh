#!/bin/sh

docker build --no-cache=true -t vipshop/saturn-executor:master-SNAPSHOT ./saturn-executor/
docker build --no-cache=true -t vipshop/saturn-console:master-SNAPSHOT ./saturn-console/

docker-compose up