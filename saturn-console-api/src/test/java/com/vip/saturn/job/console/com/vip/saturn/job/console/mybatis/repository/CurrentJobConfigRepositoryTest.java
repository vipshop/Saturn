package com.vip.saturn.job.console.com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import com.vip.saturn.job.console.mybatis.repository.CurrentJobConfigRepository;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Ray Leung
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:applicationContext.xml")
public class CurrentJobConfigRepositoryTest {

	@Autowired
	private CurrentJobConfigRepository currentJobConfigRepository;

	@Test
	@Transactional
	public void should_return_single_result_when_one_queue_per_job() {
		//Given
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName("JobA");
		jobConfig4DB.setNamespace("NamespaceA");
		jobConfig4DB.setQueueName("QueueA");
		jobConfig4DB.setLoadLevel(1);
		jobConfig4DB.setJobDegree(1);
		jobConfig4DB.setTimeZone("GMT");
		jobConfig4DB.setTimeout4AlarmSeconds(4);
		jobConfig4DB.setRerun(false);
		jobConfig4DB.setUpStream("none");
		jobConfig4DB.setDownStream("none");
		currentJobConfigRepository.insert(jobConfig4DB);
		//When
		List<JobConfig4DB> jobConfigs = currentJobConfigRepository.findConfigsByQueue("QueueA");
		//Then
		Assert.assertEquals(1, jobConfigs.size());
		Assert.assertEquals(jobConfigs.get(0).getJobName(), "JobA");
		Assert.assertEquals(jobConfigs.get(0).getNamespace(), "NamespaceA");
		Assert.assertEquals(jobConfigs.get(0).getQueueName(), "QueueA");
	}

	@Test
	@Transactional
	public void should_return_multi_result_when_one_queue_for_multi_job() {
		//Given
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName("JobA");
		jobConfig4DB.setNamespace("NamespaceA");
		jobConfig4DB.setQueueName("QueueA");
		jobConfig4DB.setLoadLevel(1);
		jobConfig4DB.setJobDegree(1);
		jobConfig4DB.setTimeZone("GMT");
		jobConfig4DB.setTimeout4AlarmSeconds(4);
		jobConfig4DB.setRerun(false);
		jobConfig4DB.setUpStream("none");
		jobConfig4DB.setDownStream("none");
		currentJobConfigRepository.insert(jobConfig4DB);

		JobConfig4DB jobConfig4DB2 = new JobConfig4DB();
		jobConfig4DB2.setJobName("JobB");
		jobConfig4DB2.setNamespace("NamespaceB");
		jobConfig4DB2.setQueueName("QueueA");
		jobConfig4DB2.setLoadLevel(1);
		jobConfig4DB2.setJobDegree(1);
		jobConfig4DB2.setTimeZone("GMT");
		jobConfig4DB2.setTimeout4AlarmSeconds(4);
		jobConfig4DB2.setRerun(false);
		jobConfig4DB2.setUpStream("none");
		jobConfig4DB2.setDownStream("none");
		currentJobConfigRepository.insert(jobConfig4DB2);
		//When
		List<JobConfig4DB> jobConfigs = currentJobConfigRepository.findConfigsByQueue("QueueA");
		//Then
		Assert.assertEquals(2, jobConfigs.size());
		Assert.assertEquals(jobConfigs.get(0).getJobName(), "JobA");
		Assert.assertEquals(jobConfigs.get(0).getNamespace(), "NamespaceA");
		Assert.assertEquals(jobConfigs.get(0).getQueueName(), "QueueA");
		Assert.assertEquals(jobConfigs.get(1).getJobName(), "JobB");
		Assert.assertEquals(jobConfigs.get(1).getNamespace(), "NamespaceB");
		Assert.assertEquals(jobConfigs.get(1).getQueueName(), "QueueA");
	}

	@Test
	@Transactional
	public void should_return_null_when_no_queue_exsited() {
		List<JobConfig4DB> jobConfigs = currentJobConfigRepository.findConfigsByQueue("QueueA");
		Assert.assertEquals(jobConfigs.size(), 0);
	}
}
