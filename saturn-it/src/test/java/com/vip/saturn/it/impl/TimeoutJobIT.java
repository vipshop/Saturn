/**
 * vips Inc.
 * Copyright (c) 2016 All Rights Reserved.
 */   
package com.vip.saturn.it.impl;   

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.util.Collection;

import org.apache.commons.exec.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.LongtimeJavaJob;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.utils.ScriptPidUtils;

public class TimeoutJobIT extends AbstractSaturnIT {
	public static String LONG_TIME_PHP_PATH;
	
	@BeforeClass
    public static void setUp() throws Exception {
        startNamespaceShardingManagerList(1);
        startExecutorList(3);
        
		File file1 = new File("src/test/resources/script/normal/longtime.php");
		LONG_TIME_PHP_PATH = file1.getAbsolutePath();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopExecutorList();
        stopNamespaceShardingManagerList();
    }
    
    @Test
    public void test_A_JavaJob() throws InterruptedException{
    	final int shardCount = 3;
    	final String jobName = "timeoutITJobJava";
    	for(int i=0;i<shardCount;i++){
    		String key = jobName+"_"+i;
    		LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
        	status.runningCount = 0;
        	status.sleepSeconds = 30;
        	status.finished = false;
        	status.timeout = false;
        	status.beforetimeout = false;
        	LongtimeJavaJob.statusMap.put(key, status);
    	}
    	
    	JobConfiguration jobConfiguration = new JobConfiguration(jobName);
    	jobConfiguration.setCron("0 0 1 * * ?");
    	jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
    	jobConfiguration.setJobClass(LongtimeJavaJob.class.getCanonicalName());
    	jobConfiguration.setTimeoutSeconds(3);
    	jobConfiguration.setShardingTotalCount(shardCount);
    	jobConfiguration.setShardingItemParameters("0=0,1=1,2=2");
    	addJob(jobConfiguration);
    	Thread.sleep(1000);
    	enableJob(jobConfiguration.getJobName());  
    	Thread.sleep(1000);
    	runAtOnce(jobName);

    	try {
			waitForFinish(new FinishCheck(){
				@Override
				public boolean docheck() {

					Collection<LongtimeJavaJob.JobStatus> values = LongtimeJavaJob.statusMap.values();
		    		for(LongtimeJavaJob.JobStatus status:values){
		    			if(!status.finished || !status.timeout || !status.beforetimeout){
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
    	
    	try {
			waitForFinish(new FinishCheck(){
				@Override
				public boolean docheck() {

			    	for(int j=0;j<shardCount;j++){
			    		if(!regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getTimeoutNode(j)))){
			    			return false;
			    		}
			    	}
					return true;
				}
				
			},10);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

    	Thread.sleep(1000);
    	for(int j=0;j<shardCount;j++){
    		String key = jobName+"_"+j;
    		LongtimeJavaJob.JobStatus status = LongtimeJavaJob.statusMap.get(key);
        	assertThat(status.runningCount).isEqualTo(0);
    		assertThat(regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getTimeoutNode(j)))).isEqualTo(true);
    		assertThat(regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getCompletedNode(j)))).isEqualTo(true);
    	}
    	
    	removeJob(jobConfiguration.getJobName());
    	LongtimeJavaJob.statusMap.clear();
    }
    
    @Test
    public void test_B_PhpJob() throws InterruptedException{
    	if (!OS.isFamilyUnix()) {
    		return;
    	}
    	final int shardCount = 3;
    	final String jobName = "timeoutITJobPhp";

    	JobConfiguration jobConfiguration = new JobConfiguration(jobName);
    	jobConfiguration.setCron("* * 1 * * ?");
    	jobConfiguration.setJobType(JobType.SHELL_JOB.toString());
    	jobConfiguration.setJobClass(LongtimeJavaJob.class.getCanonicalName());
    	jobConfiguration.setTimeoutSeconds(3);
    	jobConfiguration.setShardingTotalCount(shardCount);
    	jobConfiguration.setShardingItemParameters("0=php "+LONG_TIME_PHP_PATH+",1=php "+LONG_TIME_PHP_PATH+",2=php "+LONG_TIME_PHP_PATH);
    	addJob(jobConfiguration);
    	Thread.sleep(1000);
    	enableJob(jobConfiguration.getJobName());    	
    	Thread.sleep(1000);
    	runAtOnce(jobName);
    	
    	
    	try {
			waitForFinish(new FinishCheck(){

				@Override
				public boolean docheck() {

		            for(int i=0; i<saturnExecutorList.size(); i++) {
		            	Main saturnContainer = saturnExecutorList.get(i);
		            	for(int j=0;j<shardCount;j++){
		            		long pid = ScriptPidUtils.getFirstPidFromFile(saturnContainer.getExecutorName(), jobName,""+ j);
		            		if(pid > 0 && ScriptPidUtils.isPidRunning(pid)){
		            			return false;
		            		}	            		
		            	}
		            }

					return true;
				}
				
			},10);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
    	
    	try {
			waitForFinish(new FinishCheck(){
				@Override
				public boolean docheck() {

			    	for(int j=0;j<3;j++){
			    		if(!regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getCompletedNode(j)))){
			    			return false;
			    		}
			    	}
					return true;
				}
				
			},10);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
    	
    	removeJob(jobConfiguration.getJobName());
    	LongtimeJavaJob.statusMap.clear();
    }
}
  