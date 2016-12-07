#!/bin/sh

java -DVIP_SATURN_ZK_CONNECTION=zookeeper:2181 -cp $LIB_JARS demo/InitSaturnJob