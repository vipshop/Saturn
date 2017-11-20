package com.vip.saturn.it.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Condition;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.sharding.ShardingNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.utils.ItemUtils;

/**
 * @author hebelala
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShardingWithTrafficIT extends AbstractSaturnIT {

    @BeforeClass
    public static void setUp() throws Exception {
        startSaturnConsoleList(1);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopExecutorList();
        stopSaturnConsoleList();
    }

    /**
     * 一般流量摘取和恢复流程：<br>
     * 两个启用状态的作业，两台机A、B；<br>
     * 摘取B的流量，结果B的分片被分配到A；<br>
     * 下线B，分片分配依然不变；<br>
     * 上线B，分配分配依然不变；<br>
     * 恢复B的流量，结果平均分配分片到A、B。
     */
    @Test
    public void test_A_NormalFlow() throws Exception {
        String jobName = "test_A_NormalFlow";
        String jobName2 = "test_A_NormalFlow2";

        final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
        jobConfiguration.setCron("* * * * * ? 2099");
        jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
        jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
        jobConfiguration.setShardingTotalCount(2);
        jobConfiguration.setShardingItemParameters("0=0,1=1");

        final JobConfiguration jobConfiguration2 = new JobConfiguration(jobName2);
        jobConfiguration2.setCron("* * * * * ? 2099");
        jobConfiguration2.setJobType(JobType.JAVA_JOB.toString());
        jobConfiguration2.setJobClass(SimpleJavaJob.class.getCanonicalName());
        jobConfiguration2.setShardingTotalCount(2);
        jobConfiguration2.setShardingItemParameters("0=0,1=1");

        addJob(jobConfiguration);
        Thread.sleep(1000L);

        addJob(jobConfiguration2);
        Thread.sleep(1000L);

        enableJob(jobName);
        Thread.sleep(1000L);

        enableJob(jobName2);
        Thread.sleep(1000L);

        Main executor1 = startOneNewExecutorList();
        String executorName1 = executor1.getExecutorName();

        Main executor2 = startOneNewExecutorList();
        String executorName2 = executor2.getExecutorName();

        runAtOnceAndWaitShardingCompleted(jobConfiguration);
        runAtOnceAndWaitShardingCompleted(jobConfiguration2);
        isItemsBalanceOk(jobName, jobName2, executorName1, executorName2);

        extractTraffic(executorName2);
        Thread.sleep(1000L);

        runAtOnceAndWaitShardingCompleted(jobConfiguration);
        runAtOnceAndWaitShardingCompleted(jobConfiguration2);
        isItemsToExecutor1(jobName, jobName2, executorName1, executorName2);

        stopExecutor(1);
        Thread.sleep(1000L);

        runAtOnceAndWaitShardingCompleted(jobConfiguration);
        runAtOnceAndWaitShardingCompleted(jobConfiguration2);
        isItemsToExecutor1(jobName, jobName2, executorName1, executorName2);

        executor2 = startExecutor(1);
        executorName2 = executor2.getExecutorName();

        runAtOnceAndWaitShardingCompleted(jobConfiguration);
        runAtOnceAndWaitShardingCompleted(jobConfiguration2);
        isItemsToExecutor1(jobName, jobName2, executorName1, executorName2);

        recoverTraffic(executorName2);
        Thread.sleep(1000L);

        runAtOnceAndWaitShardingCompleted(jobConfiguration);
        runAtOnceAndWaitShardingCompleted(jobConfiguration2);
        isItemsBalanceOk(jobName, jobName2, executorName1, executorName2);

        // 清理，不影响其他Test
        disableJob(jobName);
        disableJob(jobName2);
        Thread.sleep(1000L);
        removeJob(jobName);
        removeJob(jobName2);
        Thread.sleep(1000L);
        stopExecutorList();
        Thread.sleep(2000L);
        forceRemoveJob(jobName);
        forceRemoveJob(jobName2);
    }

    private void isItemsBalanceOk(String jobName, String jobName2, String executorName1, String executorName2) throws Exception {
        List<Integer> itemsJ1E1 = ItemUtils.toItemList(regCenter.getDirectly(
                JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executorName1))));
        List<Integer> itemsJ1E2 = ItemUtils.toItemList(regCenter.getDirectly(
                JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executorName2))));
        List<Integer> itemsJ2E1 = ItemUtils.toItemList(regCenter.getDirectly(
                JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executorName1))));
        List<Integer> itemsJ2E2 = ItemUtils.toItemList(regCenter.getDirectly(
                JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executorName2))));

        List<Integer> allItems = new ArrayList<>();
        allItems.addAll(itemsJ1E1);
        allItems.addAll(itemsJ2E1);
        assertThat(allItems).hasSize(2);
        allItems.clear();
        allItems.addAll(itemsJ1E2);
        allItems.addAll(itemsJ2E2);
        assertThat(allItems).hasSize(2);
        allItems.clear();
        allItems.addAll(itemsJ1E1);
        allItems.addAll(itemsJ2E1);
        allItems.addAll(itemsJ1E2);
        allItems.addAll(itemsJ2E2);
        assertThat(allItems)
                .hasSize(4)
                .haveExactly(2, new Condition<Integer>() {
                    @Override
                    public boolean matches(Integer value) {
                        return value == 0;
                    }
                })
                .haveExactly(2, new Condition<Integer>() {
                    @Override
                    public boolean matches(Integer value) {
                        return value == 1;
                    }
                });
    }

    private void isItemsToExecutor1(String jobName, String jobName2, String executorName1, String executorName2) throws Exception {
        List<Integer> itemsJ1E1 = ItemUtils.toItemList(regCenter.getDirectly(
                JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executorName1))));
        List<Integer> itemsJ1E2 = ItemUtils.toItemList(regCenter.getDirectly(
                JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executorName2))));
        List<Integer> itemsJ2E1 = ItemUtils.toItemList(regCenter.getDirectly(
                JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executorName1))));
        List<Integer> itemsJ2E2 = ItemUtils.toItemList(regCenter.getDirectly(
                JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executorName2))));

        List<Integer> allItems = new ArrayList<>();
        allItems.addAll(itemsJ1E1);
        allItems.addAll(itemsJ2E1);
        assertThat(allItems)
                .hasSize(4)
                .haveExactly(2, new Condition<Integer>() {
                    @Override
                    public boolean matches(Integer value) {
                        return value == 0;
                    }
                })
                .haveExactly(2, new Condition<Integer>() {
                    @Override
                    public boolean matches(Integer value) {
                        return value == 1;
                    }
                });
        allItems.clear();
        allItems.addAll(itemsJ1E2);
        allItems.addAll(itemsJ2E2);
        assertThat(allItems).isEmpty();
    }
}

