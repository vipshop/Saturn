@echo off
cd ..
echo Buidling Staturn....
call mvn package --projects saturn-console -Dmaven.javadoc.skip=true -Dmaven.test.skip=true 

set json_path=%cd%
set REG_CENTER_JSON_PATH=%json_path%\bin\quickstart-json.txt
echo "Running Saturn Console, view it later at http://localhost:9088"

start "saturn console" java -Dsaturn.embeddedzk=true -Dsaturn.stdout=true -jar saturn-console/target/saturn-console-master-SNAPSHOT.jar
echo done





