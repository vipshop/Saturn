/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.repository.zookeeper;

import java.util.Collection;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorTransactionResult;

import com.vip.saturn.job.console.utils.BooleanWrapper;
import org.apache.zookeeper.data.Stat;

public interface CuratorRepository {

	CuratorFramework connect(String connectString, String namespace, String digest);

	interface CuratorFrameworkOp {

		boolean checkExists(String znode);

		String getData(String znode);

		List<String> getChildren(String znode);

		void create(String znode);

		void create(final String znode, Object value);

		void update(String znode, Object value);

		void delete(String znode);

		void deleteRecursive(String znode);

		void fillJobNodeIfNotExist(String node, Object value);

		Stat getStat(String node);

		long getMtime(String node);

		long getCtime(String node);

		CuratorTransactionOp inTransaction();

		CuratorFramework getCuratorFramework();

		interface CuratorTransactionOp {

			CuratorTransactionOp replaceIfchanged(String znode, Object value) throws Exception;

			CuratorTransactionOp replaceIfchanged(String znode, Object value, BooleanWrapper bw) throws Exception;

			CuratorTransactionOp create(String znode) throws Exception;

			Collection<CuratorTransactionResult> commit() throws Exception;
		}

	}

	CuratorFrameworkOp inSessionClient();

	CuratorFrameworkOp newCuratorFrameworkOp(CuratorFramework curatorFramework);

}
