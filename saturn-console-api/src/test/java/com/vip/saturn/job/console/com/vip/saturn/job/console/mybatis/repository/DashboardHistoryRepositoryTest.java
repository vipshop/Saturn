/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

package com.vip.saturn.job.console.com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.DashboardHistory;
import com.vip.saturn.job.console.mybatis.repository.DashboardHistoryRepository;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Ray Leung
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:applicationContext.xml")
public class DashboardHistoryRepositoryTest {

	@Autowired
	private DashboardHistoryRepository dashboardHistoryRepository;

	@Test
	@Ignore
	@Transactional
	public void testCreateHistory() {
		dashboardHistoryRepository.createOrUpdateHistory("zk", "Domain", "DomainStatistics", "somecontent", new Date());
		List<String> zkClusters = new ArrayList<>();
		zkClusters.add("zk");
		List<DashboardHistory> dashboardHistories = dashboardHistoryRepository
				.selectByZkClustersAndTypeAndTopicAndFromStartDateToEndDate(zkClusters, "Domain", "DomainStatistics",
						new Date(), new Date());
		Assert.assertEquals(1, dashboardHistories.size());
	}

	@Test
	@Ignore
	@Transactional
	public void testUpdateHistory() {
		dashboardHistoryRepository.createOrUpdateHistory("zk", "Domain", "DomainStatistics", "somecontent", new Date());
		List<String> zkClusters = new ArrayList<>();
		zkClusters.add("zk");
		List<DashboardHistory> dashboardHistories = dashboardHistoryRepository
				.selectByZkClustersAndTypeAndTopicAndFromStartDateToEndDate(zkClusters, "Domain", "DomainStatistics",
						new Date(), new Date());
		Assert.assertEquals(1, dashboardHistories.size());
		dashboardHistoryRepository.createOrUpdateHistory("zk", "Domain", "DomainStatistics", "newcontent", new Date());
		dashboardHistories = dashboardHistoryRepository
				.selectByZkClustersAndTypeAndTopicAndFromStartDateToEndDate(zkClusters, "Domain", "DomainStatistics",
						new Date(),

						new Date());
		Assert.assertEquals(1, dashboardHistories.size());
	}

	@Test
	@Ignore
	@Transactional
	public void testQueryFromToDate() {
		Calendar yesterday = Calendar.getInstance();
		yesterday.add(Calendar.DATE, -1);
		Calendar theDayBeforeYesterday = Calendar.getInstance();
		theDayBeforeYesterday.add(Calendar.DATE, -2);
		List<String> zkClusters = new ArrayList<>();
		zkClusters.add("zk");
		dashboardHistoryRepository
				.createOrUpdateHistory("zk", "Domain", "DomainStatistics", "somecontent", yesterday.getTime());
		dashboardHistoryRepository.createOrUpdateHistory("zk", "Domain", "DomainStatistics", "somecontent",
				theDayBeforeYesterday.getTime());
		dashboardHistoryRepository.createOrUpdateHistory("zk", "Domain", "DomainStatistics", "somecontent", new Date());
		List<DashboardHistory> dashboardHistories = dashboardHistoryRepository
				.selectByZkClustersAndTypeAndTopicAndFromStartDateToEndDate(zkClusters, "Domain", "DomainStatistics",
						theDayBeforeYesterday.getTime(), yesterday.getTime());
		Assert.assertEquals(2, dashboardHistories.size());
	}

	@Test
	@Ignore
	@Transactional
	public void testBatchCreate() {
		List<DashboardHistory> dashboardHistories = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			DashboardHistory dashboardHistory = new DashboardHistory("zk", "type" + String.valueOf(i), "topic",
					String.valueOf(i), new Date());
			dashboardHistories.add(dashboardHistory);
		}
		dashboardHistoryRepository.batchCreateOrUpdateHistory(dashboardHistories);
		List<String> zkClusters = new ArrayList<>();
		zkClusters.add("zk");
		List<DashboardHistory> result = dashboardHistoryRepository
				.selectByZkClustersAndTypeAndTopicAndFromStartDateToEndDate(zkClusters, null, null, null, null);
		Assert.assertEquals(5, result.size());
	}

	@Test
	@Ignore
	@Transactional
	public void testBatchUpdate() {
		List<DashboardHistory> dashboardHistories = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			DashboardHistory dashboardHistory = new DashboardHistory("zk", "type" + String.valueOf(i), "topic", "hello",
					new Date());
			dashboardHistories.add(dashboardHistory);
		}
		dashboardHistoryRepository.batchCreateOrUpdateHistory(dashboardHistories);

		for (int i = 0; i < 2; i++) {
			DashboardHistory dashboardHistory = new DashboardHistory("zk", "type" + String.valueOf(i), "topic",
					"hello world", new Date());
			dashboardHistories.add(dashboardHistory);
		}
		dashboardHistoryRepository.batchCreateOrUpdateHistory(dashboardHistories);

		List<String> zkClusters = new ArrayList<>();
		zkClusters.add("zk");

		List<DashboardHistory> result = dashboardHistoryRepository
				.selectByZkClustersAndTypeAndTopicAndFromStartDateToEndDate(zkClusters, null, null, null, null);

		for (DashboardHistory history : result) {
			Assert.assertEquals("hello world", history.getContent());
		}
	}
}
