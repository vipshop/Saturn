#!/bin/bash

if [ $# -lt 1 ]; then
  echo "Pleas input the full release version. e.g ./release-jenkins.sh  0.0.1-RC1"
  exit -1
fi

PRG="$0"
PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR/.." >/dev/null; pwd`

cd ${BASEDIR}

#BRANCHNAME=$1

FULLVERSION=$1

PUSH_TARGET=
if [ $# -gt 1 ]; then
	PUSH_TARGET=$2
fi


#git checkout $BRANCHNAME

echo "[step 1/8] Replace master-SNAPSHOT to ${FULLVERSION} in all the pom.xml"

for i in `find . -name pom.xml`; do
  sed -i "s|master-SNAPSHOT|${FULLVERSION}|g" $i
done

for i in `find . -name quickstart.sh`; do
  sed -i "s|master-SNAPSHOT|${FULLVERSION}|g" $i
done

for i in `find . -name quickstart.bat`; do
  sed -i "s|master-SNAPSHOT|${FULLVERSION}|g" $i
done

echo "[step 2/8] Replace build.version=saturn-dev to ${FULLVERSION} in saturn-core.properties"

for i in `find . -name saturn-core.properties`; do
  sed -i "s/build.version=saturn-dev/build.version=${FULLVERSION}/g" $i
done

echo "[step 3/8] Replace module.version=saturn-dev to ${FULLVERSION} in saturn-job.module"

for i in `find . -name saturn-job.module`; do
  sed -i "s/module.version=saturn-dev/module.version=${FULLVERSION}/g" $i
done

echo "[step 4/8] Replace build.version=saturn-dev and console.version=saturn-dev to ${FULLVERSION} in application.properties"

for i in `find . -name application.properties`; do
  sed -i "s/build.version=saturn-dev/build.version=${FULLVERSION}/g" $i
  sed -i "s/console.version=saturn-dev/console.version=${FULLVERSION}/g" $i
done
