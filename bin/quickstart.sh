#!/bin/sh
PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR/.." >/dev/null; pwd`
cd $BASEDIR

echo "1.Buidling Staturn...."
mvn clean package -Dmaven.javadoc.skip=true -Dmaven.test.skip=true 


echo "2.Running Saturn Console, view it later at http://localhost:9088"
export REG_CENTER_JSON_PATH=${BASEDIR}/bin/quickstart-json.txt
nohup java -Dsaturn.embeddedzk=true -Dsaturn.stdout=true -jar saturn-console/target/saturn-console-master-SNAPSHOT.jar > ./saturn-console.log 2>&1 &
sleep 10

echo "3.Running Saturn Executor"
cd saturn-executor/target
export VIP_SATURN_ZK_CONNECTION=localhost:2182
jar xf saturn-executor-master-SNAPSHOT-zip.zip
cp $BASEDIR/bin/demo/demo-java-job.jar -r $BASEDIR/saturn-executor/target/saturn-executor-master-SNAPSHOT/lib
nohup java -Xms256m -Xmx256m -Xss256K -Dsaturn.log=saturn-job-executor.log -Dlog.folder=executor-1 -Dsaturn.stdout=true -Dstart.check.outfile=status  -jar saturn-executor-master-SNAPSHOT.jar  -namespace mydomain -executorName executor-1 -saturnLibDir $BASEDIR/saturn-executor/target/saturn-executor-master-SNAPSHOT -appLibDir $BASEDIR/saturn-executor/target/saturn-executor-master-SNAPSHOT/lib > ./saturn-executor.log 2>&1 & 
sleep 10

echo "4.Add a java job"
cd $BASEDIR/saturn-executor/target/saturn-executor-master-SNAPSHOT
cp $BASEDIR/bin/demo -r $BASEDIR/saturn-executor/target/saturn-executor-master-SNAPSHOT
export LIB_JARS=lib/*:$CLASSPATH
java -cp $LIB_JARS demo/DemoJavaJob
echo "5.done, visit http://localhost:9088 for more"