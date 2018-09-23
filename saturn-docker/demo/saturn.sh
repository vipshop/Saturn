#!/bin/bash

#sleep seconds to wait for console start up
sleep 35

# provision the demo job config
curl -H "Content-type: application/json" -X POST -d '{"jobName":"demoJavaJob","description":"Java Job Sample","jobConfig":{"jobClass":"demo.DemoJavaJob","jobParameter":"","jobType":"JAVA_JOB","cron":"0/5 * * * * ?","loadLevel":1,"localMode":false,"shardingItemParameters":"0=0,1=1,2=2,3=3,4=4","shardingTotalCount":5,"timeout4AlarmSeconds":0,"timeoutSeconds":0,"useDispreferList":true}}' http://console:9088/rest/v1/saturn-it.vip.com/jobs

sleep 11

# enable job
curl -X POST http://console:9088/rest/v1/saturn-it.vip.com/jobs/demoJavaJob/enable
# start executor
chmod +x /saturn-executor/bin/saturn-executor.sh
/saturn-executor/bin/saturn-executor.sh start -n saturn-it.vip.com -r foreground -env docker -d /saturn-executor/apps -DVIP_SATURN_CONSOLE_URI=http://console:9088
