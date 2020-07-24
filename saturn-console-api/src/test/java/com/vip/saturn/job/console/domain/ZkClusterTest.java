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
