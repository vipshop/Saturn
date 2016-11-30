/**
 * vips Inc.
 * Copyright (c) 2016 All Rights Reserved.
 */   
package com.vip.saturn.it.impl;   

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.LongtimeJavaJob;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.storage.JobNodePath;

/**
 * 项目名称：saturn-job-it 
 * 创建时间：2016年7月26日 上午11:32:56   
 * @author yangjuanying  
 * @version 1.0   
 * @since JDK 1.7.0_05  
 * 文件名称：ForceStopJobIT.java  
 * 类说明：  
 */
public class ForceStopJobIT extends AbstractSaturnIT {

	@BeforeClass
    public static void setUp() throws Exception {
        startNamespaceShardingManagerList(1);
        startExecutorList(3);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopExecutorList();
        stopNamespaceShardingManagerList();
    }
    
    /**
     * 作业STOPPING时立即强制终止
     * @throws InterruptedException
     */
    @Test
    public void test_A() throws InterruptedException{
    	final int shardCount = 3;
    	final String jobName = "forceStopITJob";
    	
    	for(int i=0;i<shardCount;i++){
    		String key = jobName+"_"+i;
    		LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
        	status.runningCount = 0;
        	status.sleepSeconds = 60;
        	status.finished = false;
        	status.killed = false;
        	status.timeout = false;
        	LongtimeJavaJob.statusMap.put(key, status);
    	}
    	
    	
    	JobConfiguration jobConfiguration = new JobConfiguration(jobName);
    	jobConfiguration.setCron("0 0 * * * ?");
    	jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
    	jobConfiguration.setJobClass(LongtimeJavaJob.class.getCanonicalName());
    	jobConfiguration.setShardingTotalCount(shardCount);
    	jobConfiguration.setTimeoutSeconds(0);
    	jobConfiguration.setShardingItemParameters("0=0,1=1,2=2");
    	addJob(jobConfiguration);
    	Thread.sleep(1000);
    	enableJob(jobConfiguration.getJobName());    
    	Thread.sleep(1000);
    	runAtOnce(jobName);
    	Thread.sleep(2000);
    	disableJob(jobConfiguration.getJobName());
    	Thread.sleep(1000);
    	forceStopJob((jobConfiguration.getJobName()));
    	Thread.sleep(1000);
    	
    	try {
			waitForFinish(new FinishCheck(){

				@Override
				public boolean docheck() {
					Collection<LongtimeJavaJob.JobStatus> values = LongtimeJavaJob.statusMap.values();
		    		for(LongtimeJavaJob.JobStatus status:values){
		    			if(!status.finished || !status.killed){
		    				return false;
		    			}
		    		}
					return true;
				}
				
			},30);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
    	
    	for(int j=0;j<shardCount;j++){
    		String key = jobName+"_"+j;
    		LongtimeJavaJob.JobStatus status = LongtimeJavaJob.statusMap.get(key);
        	assertThat(status.runningCount).isEqualTo(0);
	
    		String path = JobNodePath.getNodeFullPath(jobName, ExecutionNode.getCompletedNode(j));
    		assertThat(regCenter.isExisted(path)).isEqualTo(true);
    	}
    	
    	enableJob(jobName);
    	Thread.sleep(2000);
    	
    	for(int i=0;i<shardCount;i++){
    		String key = jobName+"_"+i;
    		LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
        	status.runningCount = 0;
        	status.sleepSeconds = 1;
        	status.finished = false;
        	status.killed = false;
        	status.timeout = false;
        	LongtimeJavaJob.statusMap.put(key, status);
    	}
    	runAtOnce(jobName);
    	try {
			waitForFinish(new FinishCheck(){

				@Override
				public boolean docheck() {
					for(int i=0;i<shardCount;i++){
			    		String key = jobName+"_"+i;
			    		LongtimeJavaJob.JobStatus status = LongtimeJavaJob.statusMap.get(key);
			    		if(status.runningCount == 0){
			    			return false;
			    		}
			    	}
					return true;
				}
				
			},30);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
    	
    	removeJob(jobConfiguration.getJobName());
    	
    	LongtimeJavaJob.statusMap.clear();
    }
}
  