/**
 * Copyright 2016 vip.com. <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at <p>
 * http://www.apache.org/licenses/LICENSE-2.0 <p> Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License. </p>
 */

package com.vip.saturn.job.console.repository.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorTransactionResult;
import org.apache.zookeeper.data.Stat;

import java.util.Collection;
import java.util.List;

public interface CuratorRepository {

	CuratorFramework connect(String connectString, String namespace, String digest);

	CuratorFrameworkOp inSessionClient();

	CuratorFrameworkOp newCuratorFrameworkOp(CuratorFramework curatorFramework);

	interface CuratorFrameworkOp {

		boolean checkExists(String node);

		String getData(String node);

		List<String> getChildren(String node);

		void create(String node);

		void create(final String node, Object value);

		void update(String node, Object value);

		void delete(String node);

		void deleteRecursive(String node);

		void fillJobNodeIfNotExist(String node, Object value);

		Stat getStat(String node);

		long getMtime(String node);

		long getCtime(String node);

		CuratorTransactionOp inTransaction();

		CuratorFramework getCuratorFramework();

		interface CuratorTransactionOp {

			CuratorTransactionOp replace(String node, Object value) throws Exception;

			CuratorTransactionOp replaceIfChanged(String node, Object value) throws Exception;

			CuratorTransactionOp create(String node, Object value) throws Exception;

			CuratorTransactionOp delete(String node) throws Exception;

			Collection<CuratorTransactionResult> commit() throws Exception;
		}

	}

}
