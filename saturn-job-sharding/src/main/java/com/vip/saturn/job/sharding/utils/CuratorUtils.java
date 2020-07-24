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

package com.vip.saturn.job.sharding.utils;

import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hebelala
 */
public class CuratorUtils {

	private static final Logger log = LoggerFactory.getLogger(CuratorUtils.class);

	private CuratorUtils() {
	}

	/**
	 * Not use curator's deletingChildrenIfNeeded, to avoid this bug https://github.com/apache/curator/pull/235
	 */
	public static void deletingChildrenIfNeeded(final CuratorFramework curatorFramework, final String path)
			throws Exception {
		List<String> children;
		try {
			children = curatorFramework.getChildren().forPath(path);
		} catch (KeeperException.NoNodeException e) {
			log.debug("no node exception throws during get children of path:" + path, e);
			return;
		}

		if (children != null) {
			for (String child : children) {
				deletingChildrenIfNeeded(curatorFramework, path + "/" + child);
			}
		}

		try {
			curatorFramework.delete().guaranteed().forPath(path);
		} catch (KeeperException.NotEmptyException e) {
			log.debug("try to delete path:" + path + " but fail for NotEmptyException", e);
			deletingChildrenIfNeeded(curatorFramework, path);
		} catch (KeeperException.NoNodeException e) {
			// When multi-client delete the children concurrently, then will throw such exception. So just do nothing.
			log.debug("try to delete path:" + path + " but fail for NoNodeException", e);
		}
	}

}
