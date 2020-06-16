package com.vip.saturn.job.console.domain;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.BeanUtils;

public class ZkClusterTest {

	@Test
	public void testEqualsNoNeedReconnect() {
		ZkCluster zkCluster1 = new ZkCluster();
		zkCluster1.setZkAlias("saturn");
		zkCluster1.setZkAddr("127.0.0.1:2181");
		zkCluster1.setZkClusterKey("saturn-zk");
		zkCluster1.setDescription("cluster1");

		ZkCluster zkCluster2 = new ZkCluster();
		BeanUtils.copyProperties(zkCluster1, zkCluster2);
		zkCluster2.setDescription("cluster2");

		Assert.assertFalse(zkCluster1.equals(zkCluster2));
		Assert.assertTrue(zkCluster1.equalsNoNeedReconnect(zkCluster2));
	}

}
