package com.vip.saturn.job.internal.config;

import com.vip.saturn.job.basic.JobRegistry;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.reg.zookeeper.ZookeeperConfiguration;
import com.vip.saturn.job.reg.zookeeper.ZookeeperRegistryCenter;
import org.junit.Test;

import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;

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
        ConfigurationService configurationService = new ConfigurationService(new JobScheduler(zookeeperRegistryCenter, jobConfiguration));

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
        ConfigurationService configurationService = new ConfigurationService(new JobScheduler(zookeeperRegistryCenter, jobConfiguration));

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

        ZookeeperRegistryCenter zookeeperRegistryCenter = new ZookeeperRegistryCenter(new ZookeeperConfiguration());
        zookeeperRegistryCenter.setExecutorName("haha");
        ConfigurationService configurationService = new ConfigurationService(new JobScheduler(zookeeperRegistryCenter, jobConfiguration));

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

        ZookeeperRegistryCenter zookeeperRegistryCenter = new ZookeeperRegistryCenter(new ZookeeperConfiguration());
        zookeeperRegistryCenter.setExecutorName("haha");
        ConfigurationService configurationService = new ConfigurationService(new JobScheduler(zookeeperRegistryCenter, jobConfiguration));

        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(2016, 8, 12, 11, 40); // 注意，日期从0-11，这里实际上是9月
            boolean inPausePeriod = configurationService.isInPausePeriod(calendar.getTime());
            assertThat(inPausePeriod).isTrue();
        } finally {
            JobRegistry.clearExecutor(zookeeperRegistryCenter.getExecutorName());
        }
    }

}
