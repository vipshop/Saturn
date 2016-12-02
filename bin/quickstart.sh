#!/bin/sh
PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR/.." >/dev/null; pwd`
cd $BASEDIR

echo "Buidling Staturn...."
mvn clean package -Dmaven.javadoc.skip=true -Dmaven.test.skip=true 

export REG_CENTER_JSON_PATH=${BASEDIR}/bin/quickstart-json.txt

echo "Running Saturn Console, view it later at http://localhost:9088"
java -Dsaturn.embeddedzk=true -jar saturn-console/target/saturn-console-master-SNAPSHOT.jar
