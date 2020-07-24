/**
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.service.NamespaceZkClusterMapping4SqlService;
import com.vip.saturn.job.console.mybatis.service.ZkClusterInfoService;
import com.vip.saturn.job.console.service.SystemConfigService;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RegistryCenterServiceImplTest {

	@Mock
	private LinkedHashMap<String, ZkCluster> zkClusterMap;

	@Mock
	private NamespaceZkClusterMapping4SqlService namespaceZkClusterMapping4SqlService;

	@Mock
	private ZkClusterInfoService zkClusterInfoService;

	@Mock
	private SystemConfigService systemConfigService;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@InjectMocks
	private RegistryCenterServiceImpl registryCenterService;

	@Test
	public void testDeleteZkClusterFail() throws Exception {
		//delete fail
		when(zkClusterMap.get("empty")).thenReturn(null);
		try {
			registryCenterService.deleteZkCluster("empty");
			fail("should not be here");
		} catch (SaturnJobConsoleException e) {
			Assert.assertEquals("fail to delete.for ZkCluster does not exist", e.getMessage());
		}

		//delete fail
		ZkCluster zkCluster = new ZkCluster();
		ArrayList<RegistryCenterConfiguration> regCenterConfList = new ArrayList();
		zkCluster.setRegCenterConfList(regCenterConfList);
		regCenterConfList.add(new RegistryCenterConfiguration());
		when(zkClusterMap.get("hasDomains")).thenReturn(zkCluster);
		try {
			registryCenterService.deleteZkCluster("hasDomains");
			fail("should not be here");
		} catch (SaturnJobConsoleException e) {
			Assert.assertEquals("fail to delete.for ZkCluster still has domains", e.getMessage());
		}

		//delete fail
		when(zkClusterMap.get("noDomainsInMemory")).thenReturn(new ZkCluster());
		when(namespaceZkClusterMapping4SqlService.getAllNamespacesOfCluster("noDomainsInMemory")).thenReturn(Arrays.asList(""));
		try {
			registryCenterService.deleteZkCluster("noDomainsInMemory");
			fail("should not be here");
		} catch (SaturnJobConsoleException e) {
			Assert.assertEquals("fail to delete.for ZkCluster still has domains", e.getMessage());
		}

		//delete success
		when(namespaceZkClusterMapping4SqlService.getAllNamespacesOfCluster("noDomainsInMemory")).thenReturn(null);
		registryCenterService.deleteZkCluster("noDomainsInMemory");
		verify(zkClusterInfoService, times(1)).deleteZkCluster("noDomainsInMemory");
	}

}
