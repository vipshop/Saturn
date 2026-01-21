#!/bin/bash

cd ..

SATURN_OPTS="-DVIP_SATURN_CONSOLE_URI=http://localhost:9088 -Dsaturn.stdout=true -Dsaturn.home=saturn"

export SATURN_APP_NAMESPACE=com.saturn.devdemo
export SATURN_APP_EXECUTOR_NAME=executor_002

nohup java $SATURN_OPTS -jar lib/saturn-demo-springboot-embedded-1.0-SNAPSHOT.jar > bin/nohp.out 2>&1 &
