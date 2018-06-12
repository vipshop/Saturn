package com.vip.saturn.job.internal.config;

import com.vip.saturn.job.basic.JobRegistry;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.reg.zookeeper.ZookeeperConfiguration;
import com.vip.saturn.job.reg.zookeeper.ZookeeperRegistryCenter;
import org.junit.Test;

import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by xiaopeng.he on 2016/9/23.
 */
public class ConfigurationServiceTest {

    @Test
    public void test_A_isInPausePeriodDate() throws Exception {
        JobConfiguration jobConfiguration = new JobConfiguration("");
        jobConfiguration.setPausePeriodDate("09/11-10/01");

        ZookeeperRegistryCenter zookeeperRegistryCenter = new ZookeeperRegistryCenter(new ZookeeperConfiguration());
        zookeeperRegistryCenter.setExecutorName("haha");
        ConfigurationService configurationService = new ConfigurationService(
                new JobScheduler(zookeeperRegistryCenter, jobConfiguration));

        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(2016, 8, 12, 11, 40); // 注意，日期从0-11，这里实际上是9月
            boolean inPausePeriod = configurationService.isInPausePeriod(calendar.getTime());
            assertThat(inPausePeriod).isTrue();
        } finally {
            JobRegistry.clearExecutor(zookeeperRegistryCenter.getExecutorName());
        }
    }

    @Test
    public void test_A_isInPausePeriodDate2() throws Exception {
        JobConfiguration jobConfiguration = new JobConfiguration("");
        jobConfiguration.setPausePeriodDate("9/1-9/33,10/01-10/02");

        ZookeeperRegistryCenter zookeeperRegistryCenter = new ZookeeperRegistryCenter(new ZookeeperConfiguration());
        zookeeperRegistryCenter.setExecutorName("haha");
        ConfigurationService configurationService = new ConfigurationService(
                new JobScheduler(zookeeperRegistryCenter, jobConfiguration));

        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(2016, 8, 12, 11, 40); // 注意，日期从0-11，这里实际上是9月
            boolean inPausePeriod = configurationService.isInPausePeriod(calendar.getTime());
            assertThat(inPausePeriod).isTrue();
        } finally {
            JobRegistry.clearExecutor(zookeeperRegistryCenter.getExecutorName());
        }
    }

    @Test
    public void test_A_isInPausePeriodTime() throws Exception {
        JobConfiguration jobConfiguration = new JobConfiguration("");
        jobConfiguration.setPausePeriodTime("11:30-12:00");
        jobConfiguration.setTimeZone(TimeZone.getDefault().getID());

        ZookeeperRegistryCenter zookeeperRegistryCenter = new ZookeeperRegistryCenter(new ZookeeperConfiguration());
        zookeeperRegistryCenter.setExecutorName("haha");
        ConfigurationService configurationService = new ConfigurationService(
                new JobScheduler(zookeeperRegistryCenter, jobConfiguration));

        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(2016, 8, 12, 11, 40); // 注意，日期从0-11，这里实际上是9月
            boolean inPausePeriod = configurationService.isInPausePeriod(calendar.getTime());
            assertThat(inPausePeriod).isTrue();
        } finally {
            JobRegistry.clearExecutor(zookeeperRegistryCenter.getExecutorName());
        }
    }

    @Test
    public void test_A_isInPausePeriodDateAndTime() throws Exception {
        JobConfiguration jobConfiguration = new JobConfiguration("");
        jobConfiguration.setPausePeriodDate("09/11-10/01");
        jobConfiguration.setPausePeriodTime("11:30-12:00");
        jobConfiguration.setTimeZone(TimeZone.getDefault().getID());

        ZookeeperRegistryCenter zookeeperRegistryCenter = new ZookeeperRegistryCenter(new ZookeeperConfiguration());
        zookeeperRegistryCenter.setExecutorName("haha");
        ConfigurationService configurationService = new ConfigurationService(
                new JobScheduler(zookeeperRegistryCenter, jobConfiguration));

        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(2016, 8, 12, 11, 40); // 注意，日期从0-11，这里实际上是9月
            boolean inPausePeriod = configurationService.isInPausePeriod(calendar.getTime());
            assertThat(inPausePeriod).isTrue();
        } finally {
            JobRegistry.clearExecutor(zookeeperRegistryCenter.getExecutorName());
        }
    }

    /**
     * If sharding parameters appear nonnumeric key, the key should be dropped
     * For issue #844 fixed by ray.leung
     */
    @Test
    public void test_getShardingItemParameters_withInvalidNonnumericKey() {
        JobConfiguration jobConfiguration = mock(JobConfiguration.class);
        when(jobConfiguration.getShardingItemParameters()).thenReturn("0=1,1=2,1={aa},2=a,a=");
        JobScheduler jobScheduler = mock(JobScheduler.class);
        when(jobScheduler.getCurrentConf()).thenReturn(jobConfiguration);
        ConfigurationService configurationService = new ConfigurationService(jobScheduler);
        Map parameters = configurationService.getShardingItemParameters();
        assertNull(parameters.get("a"));
    }

}
