@echo off
cd ..
echo Buidling Staturn....
call mvn package --projects saturn-console -Dmaven.javadoc.skip=true -Dmaven.test.skip=true 

set json_path=%cd%
set REG_CENTER_JSON_PATH=%json_path%\bin\quickstart-json.txt
echo Running Saturn Console, view it later at http://localhost:9088
start "saturn console" java -Dsaturn.embeddedzk=true -Dsaturn.stdout=true -jar saturn-console/target/saturn-console-master-SNAPSHOT.jar

cd saturn-executor\target
echo Running Saturn Executor
set VIP_SATURN_ZK_CONNECTION=localhost:2182
jar xf saturn-executor-master-SNAPSHOT-zip.zip
start "saturn executor" java -Xms256m -Xmx256m -Xss256K -Dsaturn.log=saturn-job-executor.log -Dlog.folder=executor-1 -Dstart.check.outfile=status  -jar saturn-executor-master-SNAPSHOT.jar  -namespace mydomain -executorName executor-1 -saturnLibDir %cd%\saturn-executor-master-SNAPSHOT -appLibDir classes 

echo done





