#!/bin/sh
PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR/.." >/dev/null; pwd`
cd ${BASEDIR}

echo [Preparing] try to kill the existing saturn processes.

kill `pgrep -f saturn-console`
kill `pgrep -f saturn-executor-master-SNAPSHOT`


echo "[Step 1] Buidling Saturn, take a coffee."
mvn clean package -Dmaven.javadoc.skip=true -Dmaven.test.skip=true

if [ $? -ne 0 ];then
  echo "Quit because maven install fail"
  exit -1
fi

echo "[Step 2] Running Saturn Console, visit http://localhost:9088 after a few seconds"
nohup java -Dfile.encoding=UTF-8 -Dsaturn.embeddedZk=true -Dsaturn.embeddedDb=true -Dspring.h2.console.enabled=true -Dsaturn.stdout=true -jar saturn-console/target/saturn-console-master-SNAPSHOT-exec.jar > ./saturn-console.log 2>&1 &
sleep 30

echo "[Step 3] Running Saturn Executor"
cd saturn-executor/target
export CONSOLR_URI=http://localhost:9088
jar xf saturn-executor-master-SNAPSHOT-zip.zip

cp -r ${BASEDIR}/quickstart/demo-java-job.jar $BASEDIR/saturn-executor/target/saturn-executor-master-SNAPSHOT/lib
nohup java -Xms256m -Xmx256m -Xss256K -Dfile.encoding=UTF-8 -Dsaturn.stdout=true -Dstart.check.outfile=status -DVIP_SATURN_CONSOLE_URI=${CONSOLR_URI} -jar saturn-executor-master-SNAPSHOT.jar  -namespace mydomain -executorName executor-1 -saturnLibDir ./saturn-executor-master-SNAPSHOT -appLibDir ./saturn-executor-master-SNAPSHOT/lib > ./saturn-executor.log 2>&1 &
sleep 10

echo "[Step 4] Add a demo java job by code"
cd ${BASEDIR}/saturn-executor/target/saturn-executor-master-SNAPSHOT
export LIB_JARS=lib/*:$CLASSPATH
java -cp $LIB_JARS demo/DemoJavaJob

if [ $? -ne 0 ];then
  echo "Quit because add  demo java job fail"
  exit -1
fi

echo "[Step 5] Done, visit ${CONSOLR_URI} for more,and you can visit %CONSOLR_URI%/h2-console to connect to the in-memory db."
