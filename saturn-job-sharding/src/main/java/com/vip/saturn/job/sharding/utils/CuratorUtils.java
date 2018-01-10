package com.vip.saturn.job.sharding.utils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;

import java.util.List;

/**
 * @author hebelala
 */
public class CuratorUtils {

	/**
	 * Not use curator's deletingChildrenIfNeeded, to avoid this bug https://github.com/apache/curator/pull/235
	 */
	public static void deletingChildrenIfNeeded(final CuratorFramework curatorFramework, final String path)
			throws Exception {
		List<String> children;
		try {
			children = curatorFramework.getChildren().forPath(path);
		} catch (KeeperException.NoNodeException e) {
			return;
		}
		if (children != null && !children.isEmpty()) {
			for (String child : children) {
				deletingChildrenIfNeeded(curatorFramework, path + "/" + child);
			}
		}
		try {
			curatorFramework.delete().guaranteed().forPath(path);
		} catch (KeeperException.NotEmptyException e) {
			deletingChildrenIfNeeded(curatorFramework, path);
		} catch (KeeperException.NoNodeException e) {
			// ignore, the node maybe be removed by someone
		}
	}

}
