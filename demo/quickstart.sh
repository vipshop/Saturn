#!/bin/sh
PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR/.." >/dev/null; pwd`
cd ${BASEDIR}

echo [Preparing] try to kill the existing saturn processes.

kill `pgrep -f saturn-console`
kill `pgrep -f saturn-job-executor`


echo "[Step 1] Buidling Staturn, take a coffee."
mvn clean package -Dmaven.javadoc.skip=true -Dmaven.test.skip=true 


echo "[Step 2] Running Saturn Console, visit  http://localhost:9088 after a few seconds"
export REG_CENTER_JSON_PATH=${BASEDIR}/demo/quickstart-json.txt
nohup java -Dsaturn.embeddedzk=true -Dsaturn.stdout=true -jar saturn-console/target/saturn-console-master-SNAPSHOT.jar > ./saturn-console.log 2>&1 &
sleep 10

echo "[Step 3] Running Saturn Executor"
cd saturn-executor/target
export VIP_SATURN_ZK_CONNECTION=localhost:2182
jar xf saturn-executor-master-SNAPSHOT-zip.zip

cp -r ${BASEDIR}/demo/demo-java-job.jar $BASEDIR/saturn-executor/target/saturn-executor-master-SNAPSHOT/lib
nohup java -Xms256m -Xmx256m -Xss256K -Dsaturn.log=saturn-job-executor.log -Dlog.folder=executor-1 -Dsaturn.stdout=true -Dstart.check.outfile=status  -jar saturn-executor-master-SNAPSHOT.jar  -namespace mydomain -executorName executor-1 -saturnLibDir $BASEDIR/saturn-executor/target/saturn-executor-master-SNAPSHOT -appLibDir $BASEDIR/saturn-executor/target/saturn-executor-master-SNAPSHOT/lib > ./saturn-executor.log 2>&1 & 
sleep 10

echo "[Step 4] Add a demo java job"
cd ${BASEDIR}/saturn-executor/target/saturn-executor-master-SNAPSHOT
cp -r ${BASEDIR}/demo  $BASEDIR/saturn-executor/target/saturn-executor-master-SNAPSHOT
export LIB_JARS=lib/*:$CLASSPATH
java -cp $LIB_JARS demo/DemoJavaJob

echo "[Step 5] Done, visit http://localhost:9088 for more"