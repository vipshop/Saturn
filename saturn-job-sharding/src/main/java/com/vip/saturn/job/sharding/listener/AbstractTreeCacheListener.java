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

package com.vip.saturn.job.sharding.listener;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;

/**
 * @author hebelala 
 */
public abstract class AbstractTreeCacheListener implements TreeCacheListener {

	@Override
	public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
		ChildData data = event.getData();
		if (data != null) {
			TreeCacheEvent.Type type = event.getType();
			String path = data.getPath();
			String nodeData = null;
			byte[] dataData = data.getData();
			if (dataData != null) {
				nodeData = new String(dataData, "UTF-8");
			}
			if (path != null) {
				childEvent(type, path, nodeData);
			}
		}
	}

	public abstract void childEvent(TreeCacheEvent.Type type, String path, String nodeData) throws Exception;

}
