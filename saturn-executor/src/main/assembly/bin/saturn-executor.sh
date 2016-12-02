#!/bin/bash

ulimit -s 20280
ulimit -c unlimited
ulimit -n 20480

export PATH=$PATH:/usr/sbin

#PRG="$0"
PRG=$(readlink -f $0)
PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR/.." >/dev/null; pwd`
PARENTDIR=`cd "$BASEDIR/.." >/dev/null; pwd`

LOGDIR=""
OUTFILE=""
NAMESPACE=""
EXECUTORNAME=`hostname`
LOCALIP=`ip addr| grep 'inet '| grep -v '127.0.0.1'`
LOCALIP=`echo $LOCALIP | cut -d/ -f1|awk '{print $2}'`
JMX_PORT="24501"
MONITOR_PORT=4499
START_TIME=20
SATURN_LIB_DIR=$BASEDIR/lib
APP_LIB_DIR=$PARENTDIR/app

STATUS_FILE=${PRGDIR}/status
PID_FILE=${PRGDIR}/PID

USAGE()
{
	echo "Usage: $0 start|stop [-n|--namespace namespace] [-e|--executorName executorName] [-m|--monport monitorport] [-jmx|--jmx-port port] [JVM args, e.g., -Xms2048m -DVIP_SATURN_RUNNING_IP=192.168.1.100. Note that additional arguments should be put in the end.]"
	echo -e "\n      '-n|--namespace': required."
	echo -e "\n      '-e|--executorName': optional,default value is ${EXECUTORNAME}."
	echo -e "\n      '-m|--monport': optional,default value is  ${MONITOR_PORT}."
	echo -e "\n      '-d|--libdir': optional, default value is $PARENTDIR/app."
	echo -e "\n      '-jmx|--jmx-port': optional, default value is ${JMX_PORT}."
	echo -e "\n      '-env|--environment': optional."
	echo -e "\n      JVM args: optional."
}

if [ $# -lt 1 ]; then
	USAGE
	exit -1
fi

CMD="$1"
shift

while true; do
	case "$1" in
		-n|--namespace) NAMESPACE="$2"; shift 2;;
		-e|--executorName) EXECUTORNAME="$2"; shift 2;;
		-m|--monport) MONITOR_PORT="$2"; shift 2;;
		-d| --libdir) APP_LIB_DIR="$2"; shift 2;;
		-jmx|--jmx-port) JMX_PORT="$2" ; shift 2 ;;
		-env|--environment) RUN_ENVIRONMENT="$2" ; shift 2 ;;
		*) break;;
	esac
done

ADDITIONAL_OPTS=$*;

PERM_SIZE="256m"
MAX_PERM_SIZE="512m"

if [[ "$RUN_ENVIRONMENT" = "dev" ]]; then
  ENVIRONMENT_MEM="-Xms512m -Xmx512m -Xss256K"
  PERM_SIZE="128m"
  MAX_PERM_SIZE="256m"
elif [[ "$RUN_ENVIRONMENT" = "docker" ]]; then
  ENVIRONMENT_MEM="-Xms512m -Xmx512m -Xss256K"
  PERM_SIZE="128m"
  MAX_PERM_SIZE="256m"
else
  ENVIRONMENT_MEM="-Xms2048m -Xmx2048m"
fi


LOGDIR=/apps/logs/saturn/${NAMESPACE}/${EXECUTORNAME}-${LOCALIP}
OUTFILE=$LOGDIR/saturn-executor.log

JAVA_OPTS="-Dsaturn.log=${LOGDIR} -XX:+PrintCommandLineFlags -XX:-OmitStackTraceInFastThrow -XX:-UseBiasedLocking -XX:AutoBoxCacheMax=20000"
MEM_OPTS="-server ${ENVIRONMENT_MEM} -XX:NewRatio=1 -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -XX:+ParallelRefProcEnabled -XX:+AlwaysPreTouch -XX:MaxTenuringThreshold=6 -XX:+ExplicitGCInvokesConcurrent"
GCLOG_OPTS="-Xloggc:${LOGDIR}/gc.log  -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCDateStamps -XX:+PrintGCDetails"
CRASH_OPTS="-XX:ErrorFile=${LOGDIR}/hs_err_%p.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOGDIR}/"
JMX_OPTS="-Dcom.sun.management.jmxremote.port=${JMX_PORT} -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dsun.rmi.transport.tcp.threadKeepAliveTime=75000 -Djava.rmi.server.hostname=${LOCALIP}"
MON_CONF="-monport ${MONITOR_PORT}"
SETTING_CONF="-Dstart.check.outfile=${STATUS_FILE} -Dlog.folder=${EXECUTORNAME}-${LOCALIP}"

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')

echo -e "The java version is $JAVA_VERSION"

#if [[ "$JAVA_VERSION" < "1.7" ]]; then
#    echo "Error: Unsupported the java version $JAVA_VERSION , please use the version $TARGET_VERSION and above."
#    exit -1;
#fi

if [[ "$JAVA_VERSION" < "1.8" ]]; then
  MEM_OPTS="$MEM_OPTS -XX:PermSize=${PERM_SIZE} -XX:MaxPermSize=${MAX_PERM_SIZE} -Djava.security.egd=file:/dev/./urandom"
  JAVA_OPTS="$JAVA_OPTS -javaagent:${BASEDIR}/mercurylib/aspectjweaver-1.7.3.jar"
else         
  MEM_OPTS="$MEM_OPTS -XX:MetaspaceSize=${PERM_SIZE} -XX:MaxMetaspaceSize=${MAX_PERM_SIZE} "
  JAVA_OPTS="$JAVA_OPTS -javaagent:${BASEDIR}/mercurylib/aspectjweaver-1.8.6.jar"
fi


CHECK_MONPORT()
{
	if [ x"$MONITOR_PORT" == x ]; then
		MON_CONF=""
		return
	fi
	times=0
	nc -w 3 -z localhost ${MONITOR_PORT} > /dev/null 2>&1
	while [ $? -eq 0 ]; do
        if [ ${times} -gt 9 ]; then
                echo "failed in getting a port after 10 times. monitor will not start."
                MON_CONF="-monport -1"
                return
        fi
        echo "${MONITOR_PORT} is in use, now try `expr $MONITOR_PORT + 1`"
        ((MONITOR_PORT++))
        ((times++))
        nc -w 3 -z localhost ${MONITOR_PORT} > /dev/null 2>&1
	done
	MON_CONF="-monport ${MONITOR_PORT}"
	echo "The monitor port is ${MONITOR_PORT}."
}

CHECK_JMX()
{
	if [ x"$JMX_PORT" == x ]; then
		JMX_OPTS=""
	else
		TMP=$(echo `lsof -P -i :${JMX_PORT} | grep LISTEN | awk '{print $2}'`)
		if [ x"$TMP" != x ]; then
			echo "The jmx port is used, please use other port."
			exit -1
		fi
		echo "The jmx port is ${JMX_PORT}."
	fi
}

GET_PID()
{
	echo `ps -ef | grep java | grep "\-jar" | grep "saturn-executor.jar" | grep -v grep | awk '{print $2}' `
}

CHECK_PARAMETERS()
{
	if [ x$NAMESPACE == x ]; then
		echo -e "\nThe parameter -n|--namespace is required."
		USAGE;
		exit -1
	fi
}

START()
{
	echo "Log redirects to ${LOGDIR}"
	CHECK_JMX
	CHECK_MONPORT

	CHECK_PARAMETERS
	
	if [ ! -d $LOGDIR ]; then
		echo -e "\nWarning, the log directory of $LOGDIR is not existed, try to create it."
		mkdir -p $LOGDIR
		if [ -d $LOGDIR ]; then
			echo -e "\nCreate log directory successfully."
		else
			echo -e "\nCreate log directory failed."
			exit -1
		fi
    fi

	if [ -f $PID_FILE ] ; then
		PID=`cat $PID_FILE`
	fi
	
	if [ "$PID" != "" ]; then
		if [ -d /proc/$PID ];then
		 echo "Saturn executor is running, please stop it first!!"
		 exit -1
		fi
	fi

	echo "" > ${STATUS_FILE}
	RUN_PARAMS="-namespace ${NAMESPACE} -executorName ${EXECUTORNAME} -saturnLibDir ${SATURN_LIB_DIR} -appLibDir ${APP_LIB_DIR} $MON_CONF"
    nohup java  $JAVA_OPTS $MEM_OPTS $JMX_OPTS $GCLOG_OPTS $CRASH_OPTS $SETTING_CONF $ADDITIONAL_OPTS -jar ${BASEDIR}/saturn-executor.jar ${RUN_PARAMS}  >> $OUTFILE 2>&1 &
	PID=$!
	echo $PID > $PID_FILE
  
	sleep 3

	CHECK_STATUS=`cat ${STATUS_FILE}`
	starttime=0
	while  [ x"$CHECK_STATUS" == x ]; do
	if [[ "$starttime" -lt ${START_TIME} ]]; then
	  sleep 1
	  ((starttime++))
	  echo -e ".\c"
	  CHECK_STATUS=`cat ${STATUS_FILE}`
	else
	  echo -e "\nSaturn executor start may fails, checking not finished until reach the starting timeout! See ${OUTFILE} for more information."
	  exit -1
	fi
	done

	if [ $CHECK_STATUS = "SUCCESS" ]; then
		echo -e "\nSaturn executor start successfully, running as process:$PID."
		echo ${RUN_PARAMS} > ${STATUS_FILE}
	fi

	if [ $CHECK_STATUS = "ERROR" ]; then
		kill -9 $PID
		echo -e "\nSaturn executor start failed ! See ${OUTFILE} for more information."
		exit -1
	fi
	
}

STOP()
{	
	if [ -f $PID_FILE ] ; then
		PID=`cat $PID_FILE`
	else
		PID=$(GET_PID)
	fi

	stoptime=0  
    if [ "$PID" != "" ]; then
		if [ -d /proc/$PID ];then
			RUN_PARAMS=`cat ${STATUS_FILE}`
			echo "Saturn executor is stopping,pid is ${PID}, params are : ${RUN_PARAMS}."	
			while [ -d /proc/$PID ]; do
				if	[[ "$stoptime" -lt 300 ]];	then
					kill $PID
					sleep 1
					((stoptime++))
					echo -e ".\c"
				else
					echo -e "\nstop failed after 300 seconds. now kill -9 ${PID}"
					kill -9 $PID
				fi
			done
			echo -e "\nKill the process successfully."
		else
			echo "Saturn executor is not running."
		fi
	else
		echo -e "\nSaturn executor is not running."
	fi
}

STATUS()
{
  if [ -f $PID_FILE ] ; then
	PID=`cat $PID_FILE`
  fi
  if [ "$PID" != "" ]
	then
	if [ -d /proc/$PID ];then
	  RUN_PARAMS=`cat ${STATUS_FILE}`
	  echo "Saturn executor running ,params are : ${RUN_PARAMS}."
	  exit 0
	fi
  fi
  echo "Saturn executor is not running."
}


case "$CMD" in
  start) START;;
  stop) STOP;;
  status) STATUS;;
  help) USAGE;;
  *) USAGE;;
esac
