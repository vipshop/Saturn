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

echo "[step 5/8] Maven install"
mvn clean install -Dmaven.test.skip=true -Pprod -Dmaven.javadoc.skip=true

if [ $? -ne 0 ];then
  echo "Quit the release progress because maven install fail"
  git reset --hard
  exit -1
fi

echo "[step 6/8] Maven deploy"
mvn deploy -Dmaven.test.skip=true


echo "[step 7/8] Reset the version to master-SNAPSHOT"
git reset --hard

# git push tag by jenkins
#echo "[step 7/8] Git tag"
#git tag -a "${FULLVERSION}-tag" -m "${FULLVERSION}-tag"
#git push origin $FULLVERSION

if [ x"$PUSH_TARGET" == x ]; then
	echo "[step 8/8] Done. Won't do the merge operation."
else
	echo "[step 8/8] Merge to ${PUSH_TARGET} "
	git push --force origin HEAD:$PUSH_TARGET	
fi