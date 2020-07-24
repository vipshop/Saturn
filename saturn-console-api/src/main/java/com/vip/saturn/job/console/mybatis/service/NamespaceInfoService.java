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

import com.vip.saturn.job.console.mybatis.entity.NamespaceInfo;
import java.util.List;

/**
 * @author timmy.hu
 */
public interface NamespaceInfoService {

	NamespaceInfo selectByNamespace(String namespace);

	List<NamespaceInfo> selectAll();

	List<NamespaceInfo> selectAll(List<String> nsList);

	void create(NamespaceInfo namespaceInfo);

	void update(NamespaceInfo namespaceInfo);

	void batchCreate(List<NamespaceInfo> namespaceInfos);

	int deleteByNamespace(String namespace);

	void deleteAll();

	void replaceAll(List<NamespaceInfo> namespaceInfos);
}
