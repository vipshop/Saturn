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

echo "[step 1/6] Replace master-SNAPSHOT to ${FULLVERSION} in all the pom.xml"

for i in `find . -name pom.xml`; do
  sed -i "s|master-SNAPSHOT|${FULLVERSION}|g" $i
done

echo "[step 2/6] Replace master-SNAPSHOT to ${FULLVERSION} in all the quickstart.sh"

for i in `find . -name quickstart.sh`; do
  sed -i "s|master-SNAPSHOT|${FULLVERSION}|g" $i
done

echo "[step 3/6] Replace build.version=saturn-dev to ${FULLVERSION} in saturn-core.properties"

for i in `find . -name saturn-core.properties`; do
  sed -i "s/build.version=saturn-dev/build.version=${FULLVERSION}/g" $i
done

echo "[step 4/6] Replace module.version=saturn-dev to ${FULLVERSION} in saturn-job.module"

for i in `find . -name saturn-job.module`; do
  sed -i "s/module.version=saturn-dev/module.version=${FULLVERSION}/g" $i
done

echo "[step 5/6] Replace build.version=saturn-dev and console.version=saturn-dev to ${FULLVERSION} in application.properties"

for i in `find . -name application.properties`; do
  sed -i "s/build.version=saturn-dev/build.version=${FULLVERSION}/g" $i
  sed -i "s/console.version=saturn-dev/console.version=${FULLVERSION}/g" $i
done

echo "[step 6/6] Replace v=saturn-console-ui-version to ${FULLVERSION} in *.html"

for i in `find . -name "*.html"`; do
  sed -i "s/v=saturn-console-ui-version/v=${FULLVERSION}/g" $i
done
