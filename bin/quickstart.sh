#!/bin/sh
PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR/.." >/dev/null; pwd`
cd $BASEDIR

echo "Buidling Staturn...."
mvn clean package -Dmaven.javadoc.skip=true -Dmaven.test.skip=true 

export REG_CENTER_JSON_PATH=${BASEDIR}/bin/json.txt
export VIP_SATURN_ZK_CONNECTION=localhost:2181

echo "Running Saturn Console, see it at http://localhost:9088"
java -Dsaturn.embeddedzk=true -jar saturn-console/target/saturn-console-master-SNAPSHOT.jar
