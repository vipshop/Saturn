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

package com.vip.saturn.job.console.repository.zookeeper.impl;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.api.transaction.CuratorTransactionFinal;
import org.apache.curator.framework.api.transaction.CuratorTransactionResult;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.exception.JobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.utils.BooleanWrapper;
import com.vip.saturn.job.console.utils.ThreadLocalCuratorClient;

@Repository
public class CuratorRepositoryImpl implements CuratorRepository {

	protected static Logger log = LoggerFactory.getLogger(CuratorRepositoryImpl.class);
	private static final int WAITING_SECONDS = 2;

	/**
	 * 会话超时时间
	 */
	private static int SESSION_TIMEOUT = 20 * 1000;

	/**
	 * 连接超时时间
	 */
	private static int CONNECTION_TIMEOUT = 20 * 1000;

	@Override
	public CuratorFramework connect(final String connectString, final String namespace, final String digest) {
		Builder builder = CuratorFrameworkFactory.builder().connectString(connectString)
				.sessionTimeoutMs(SESSION_TIMEOUT).connectionTimeoutMs(CONNECTION_TIMEOUT)
				.retryPolicy(new ExponentialBackoffRetry(1000, 3, 3000));
		if (namespace != null) {
			builder.namespace(namespace);
		}
		if (!Strings.isNullOrEmpty(digest)) {
			builder.authorization("digest", digest.getBytes()).aclProvider(new ACLProvider() {

				@Override
				public List<ACL> getDefaultAcl() {
					return ZooDefs.Ids.CREATOR_ALL_ACL;
				}

				@Override
				public List<ACL> getAclForPath(final String path) {
					return ZooDefs.Ids.CREATOR_ALL_ACL;
				}
			});
		}
		CuratorFramework client = builder.build();
		client.start();
		boolean established = false;
		try {
			established = client.blockUntilConnected(WAITING_SECONDS, TimeUnit.SECONDS);
		} catch (final InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		if (established) {
			return client;
		}
		CloseableUtils.closeQuietly(client);
		return null;
	}

	class CuratorFrameworkOpImpl implements CuratorFrameworkOp {

		private CuratorFramework curatorFramework;

		public CuratorFrameworkOpImpl(CuratorFramework curatorFramework) {
			this.curatorFramework = curatorFramework;
		}

		@Override
		public boolean checkExists(final String znode) {
			try {
				return null != curatorFramework.checkExists().forPath(znode);
				// CHECKSTYLE:OFF
			} catch (final Exception ex) {
				// CHECKSTYLE:ON
				throw new JobConsoleException(ex);
			}
		}

		@Override
		public String getData(final String znode) {
			try {
				if (checkExists(znode)) {
					byte[] getZnodeData = curatorFramework.getData().forPath(znode);
					if (getZnodeData == null) {// executor的分片可能存在全部飘走的情况，sharding节点有可能获取到的是null，需要对null做判断，否则new
												// String时会报空指针异常
						return null;
					}
					return new String(getZnodeData, Charset.forName("UTF-8"));
				} else {
					return null;
				}
			} catch (final NoNodeException ex) {
				return null;
				// CHECKSTYLE:OFF
			} catch (final Exception ex) {
				// CHECKSTYLE:ON
				throw new JobConsoleException(ex);
			}
		}

		@Override
		public List<String> getChildren(final String znode) {
			try {
				return curatorFramework.getChildren().forPath(znode);
				// CHECKSTYLE:OFF
			} catch (final NoNodeException ex) {
				return null;
				// CHECKSTYLE:OFF
			} catch (final Exception ex) {
				// CHECKSTYLE:ON
				throw new JobConsoleException(ex);
			}
		}

		@Override
		public void create(final String znode) {
			create(znode, "");
		}

		@Override
		public void create(final String znode, Object data) {
			try {
				curatorFramework.create().creatingParentsIfNeeded().forPath(znode, data.toString().getBytes());
			} catch (final NodeExistsException ex) {
				// CHECKSTYLE:OFF
			} catch (final Exception ex) {
				// CHECKSTYLE:ON
				throw new JobConsoleException(ex);
			}
		}

		public void update(final String znode, final Object value) {
			try {
				if (this.checkExists(znode)) {
					curatorFramework.inTransaction().check().forPath(znode).and().setData()
							.forPath(znode, value.toString().getBytes(Charset.forName("UTF-8"))).and().commit();
				} else {
					this.create(znode, value);
				}
			} catch (final NoNodeException ex) {
				// CHECKSTYLE:OFF
			} catch (final Exception ex) {
				// CHECKSTYLE:ON
				throw new JobConsoleException(ex);
			}
		}

		@Override
		public void delete(final String znode) {
			try {
				if (null != curatorFramework.checkExists().forPath(znode)) {
					curatorFramework.delete().forPath(znode);
				}
			} catch (final NoNodeException ex) {
				// CHECKSTYLE:OFF
			} catch (final Exception ex) {
				// CHECKSTYLE:ON
				throw new JobConsoleException(ex);
			}
		}

		@Override
		public void deleteRecursive(final String znode) {
			try {
				if (null != curatorFramework.checkExists().forPath(znode)) {
					curatorFramework.delete().deletingChildrenIfNeeded().forPath(znode);
				}
			} catch (final NoNodeException ex) {
				// CHECKSTYLE:OFF
			} catch (final Exception ex) {
				// CHECKSTYLE:ON
				throw new JobConsoleException(ex);
			}
		}

		/**
		 * 如果节点不存在则填充节点数据.
		 *
		 * @param node 作业节点名称
		 * @param value 作业节点数据值
		 */
		@Override
		public void fillJobNodeIfNotExist(final String node, final Object value) {
			if (null == value) {
				log.info("job node value is null, node:{}", node);
				return;
			}
			if (!checkExists(node)) {
				try {
					curatorFramework.create().creatingParentsIfNeeded().forPath(node, value.toString().getBytes());
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}

		@Override
		public Stat getStat(String node) {
			try {
				return curatorFramework.checkExists().forPath(node);
			} catch (final Exception ex) {
				// CHECKSTYLE:ON
				throw new JobConsoleException(ex);
			}
		}

		@Override
		public long getMtime(String node) {
			try {
				Stat stat = curatorFramework.checkExists().forPath(node);
				if (stat != null) {
					return stat.getMtime();
				} else {
					return 0l;
				}
			} catch (final Exception ex) {
				// CHECKSTYLE:ON
				throw new JobConsoleException(ex);
			}
		}

		@Override
		public long getCtime(String node) {
			try {
				Stat stat = curatorFramework.checkExists().forPath(node);
				if (stat != null) {
					return stat.getCtime();
				} else {
					return 0l;
				}
			} catch (final Exception ex) {
				// CHECKSTYLE:ON
				throw new JobConsoleException(ex);
			}
		}

		/**
		 * 默认会check根路径
		 */
		@Override
		public CuratorTransactionOp inTransaction() {
			try {
				return new CuratorTransactionOpImpl(curatorFramework);
			} catch (Exception ex) {
				// CHECKSTYLE:ON
				throw new JobConsoleException(ex);
			}
		}

		@Override
		public CuratorFramework getCuratorFramework() {
			return curatorFramework;
		}

		class CuratorTransactionOpImpl implements CuratorTransactionOp {

			private CuratorTransactionFinal curatorTransactionFinal;
			private CuratorFramework curatorClient;

			public CuratorTransactionOpImpl(CuratorFramework curatorClient) {
				this.curatorClient = curatorClient;
				try {
					curatorTransactionFinal = curatorClient.inTransaction().check().forPath("/").and();
				} catch (final Exception ex) {
					throw new JobConsoleException(ex);
				}
			}

			private boolean checkExists(String znode) throws Exception {
				return curatorClient.checkExists().forPath(znode) != null;
			}

			private CuratorTransactionOpImpl create(String znode, byte[] data) throws Exception {
				curatorTransactionFinal = curatorTransactionFinal.create().forPath(znode, data).and();
				return this;
			}

			private byte[] getData(String znode) throws Exception {
				return curatorClient.getData().forPath(znode);
			}

			private byte[] toData(Object value) {
				return (value == null ? "" : value.toString()).getBytes(Charset.forName("UTF-8"));
			}

			private boolean bytesEquals(byte[] a, byte[] b) {
				if (a == null || b == null) {
					if (a == null && b == null) {
						return true;
					} else {
						return false;
					}
				}
				if (a.length != b.length) {
					return false;
				}
				for (int i = 0, size = a.length; i < size; i++) {
					if (a[i] != b[i]) {
						return false;
					}
				}
				return true;
			}

			public CuratorTransactionOpImpl replaceIfchanged(String znode, Object value) throws Exception {
				return replaceIfchanged(znode, value, new BooleanWrapper(false));
			}

			public CuratorTransactionOpImpl replaceIfchanged(String znode, Object value, BooleanWrapper ifChanged)
					throws Exception {
				byte[] newData = toData(value);
				if (this.checkExists(znode)) {
					byte[] oldData = this.getData(znode);
					if (!bytesEquals(newData, oldData)) {
						curatorTransactionFinal = curatorTransactionFinal.check().forPath(znode).and().setData()
								.forPath(znode, newData).and();
						ifChanged.setValue(true);
					}
				} else {
					this.create(znode, newData);
				}
				return this;
			}

			@Override
			public CuratorTransactionOpImpl create(String znode) throws Exception {
				curatorTransactionFinal = curatorTransactionFinal.create().forPath(znode).and();
				return this;
			}

			@Override
			public Collection<CuratorTransactionResult> commit() throws Exception {
				return curatorTransactionFinal.commit();
			}
		}

	}

	@Override
	public CuratorFrameworkOp inSessionClient() {
		return new CuratorFrameworkOpImpl(ThreadLocalCuratorClient.getCuratorClient());
	}

	@Override
	public CuratorFrameworkOp newCuratorFrameworkOp(CuratorFramework curatorFramework) {
		return new CuratorFrameworkOpImpl(curatorFramework);
	}

}
