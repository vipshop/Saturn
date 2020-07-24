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

package com.vip.saturn.job.sharding;

import ch.qos.logback.classic.Level;
import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.sharding.entity.Shard;
import com.vip.saturn.job.sharding.service.NamespaceShardingContentService;
import com.vip.saturn.job.utils.NestedZkUtils;
import org.apache.curator.framework.CuratorFramework;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by xiaopeng.he on 2016/7/12.
 */
public class NamespaceShardingContentServiceTest {

	private NestedZkUtils nestedZkUtils;

	@Before
	public void setUp() throws Exception {
		nestedZkUtils = new NestedZkUtils();
		nestedZkUtils.startServer();

	}

	@After
	public void tearDown() throws IOException {
		if (nestedZkUtils != null) {
			nestedZkUtils.stopServer();
		}
	}

	@Test
	public void testBigData() throws Exception {
		ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(NamespaceShardingContentService.class);
		logger.setLevel(Level.OFF);

		try {
			CuratorFramework framework = nestedZkUtils.createClient("namespace");
			NamespaceShardingContentService namespaceShardingContentService = new NamespaceShardingContentService(
					framework);
			List<Executor> executorList = new ArrayList<>();
			for (int i = 1; i < 250; i++) {
				Executor executor = new Executor();
				executor.setExecutorName("e" + i);
				executor.setIp("ip" + i);
				List<Shard> shardList = new ArrayList<>();
				for (int j = 1; j < 100; j++) {
					Shard shard = new Shard();
					shard.setJobName("job" + i + j);
					shard.setItem(j);
					shard.setLoadLevel(j);
					shardList.add(shard);
					executor.setTotalLoadLevel(executor.getTotalLoadLevel() + shard.getLoadLevel() * shard.getItem());
				}
				executor.setShardList(shardList);
				executorList.add(executor);
			}
			namespaceShardingContentService.persistDirectly(executorList);

			List<Executor> executorList1 = namespaceShardingContentService.getExecutorList();

			assertThat(executorList1.size()).isEqualTo(executorList.size());
		} finally {
			logger.setLevel(Level.INFO);
		}
	}

}
