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

package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.mybatis.entity.NamespaceZkClusterMapping;
import java.util.List;

/**
 * @author hebelala
 */
public interface NamespaceZkClusterMapping4SqlService {

	List<NamespaceZkClusterMapping> getAllMappings();

	List<NamespaceZkClusterMapping> getAllMappingsOfCluster(String zkClusterKey);

	List<String> getAllNamespacesOfCluster(String zkClusterKey);

	String getZkClusterKey(String namespace);

	Integer insert(String namespace, String name, String zkClusterKey, String createdBy);

	Integer remove(String namespace, String lastUpdatedBy);

	Integer update(String namespace, String name, String zkClusterKey, String lastUpdatedBy);

}
