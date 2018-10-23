package com.vip.saturn.job.console.com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.DomainCount;
import com.vip.saturn.job.console.mybatis.repository.DomainCountRepository;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Ray Leung
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:applicationContext.xml")
public class DomainCountRepositoryTest {

	@Autowired
	private DomainCountRepository domainCountRepository;

	private static EmbeddedDatabase embeddedDatabase;


	@BeforeClass
	public static void before() {
		EmbeddedDatabaseBuilder embeddedDatabaseBuilder = new EmbeddedDatabaseBuilder();
		embeddedDatabaseBuilder.setType(EmbeddedDatabaseType.H2).addScript("classpath:global.sql")
				.addScript("classpath:schema.sql");
		embeddedDatabase = embeddedDatabaseBuilder.build();
	}

	@Test
	@Transactional
	public void testCreateDomainCount() {
		domainCountRepository.createOrUpdateDomainCount("testCluster", new Date(), 0, 0);
		DomainCount domainCount = domainCountRepository.selectByZkClusterAndRecordDate("testCluster", new Date());
		Assert.assertEquals(0, domainCount.getSuccessCount());
		Assert.assertEquals(0, domainCount.getFailCount());
	}

	@Test
	@Transactional
	public void testUpdateDomainCount() {
		Date dateNow = new Date();
		domainCountRepository.createOrUpdateDomainCount("testCluster", dateNow, 0, 0);
		DomainCount domainCount = domainCountRepository.selectByZkClusterAndRecordDate("testCluster", new Date());
		Assert.assertEquals(0, domainCount.getSuccessCount());
		Assert.assertEquals(0, domainCount.getFailCount());
		domainCountRepository.createOrUpdateDomainCount("testCluster", dateNow, 10, 10);
		domainCount = domainCountRepository.selectByZkClusterAndRecordDate("testCluster", new Date());
		Assert.assertEquals(10, domainCount.getSuccessCount());
		Assert.assertEquals(10, domainCount.getFailCount());
	}

	@Test
	@Transactional
	public void testSelectByZkClusterAndFromStartDateToEndDate() {

		domainCountRepository.createOrUpdateDomainCount("testCluster", new Date(), 0, 0);
		Calendar d1 = Calendar.getInstance();
		d1.add(Calendar.DATE, -1);
		Calendar d2 = Calendar.getInstance();
		d2.add(Calendar.DATE, -2);
		domainCountRepository.createOrUpdateDomainCount("testCluster", d1.getTime(), 0, 0);
		domainCountRepository.createOrUpdateDomainCount("testCluster", d2.getTime(), 0, 0);

		List<DomainCount> domainCounts = domainCountRepository
				.selectByZkClusterAndFromStartDateToEndDate("testCluster", d2.getTime(), d1.getTime());
		Assert.assertEquals(2, domainCounts.size());
	}

}
