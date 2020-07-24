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

import com.google.gson.Gson;
import com.vip.saturn.job.console.domain.Constant;
import com.vip.saturn.job.console.domain.ZkTree;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.ZkTreeService;
import com.vip.saturn.job.console.service.helper.ReuseCallBack;
import com.vip.saturn.job.console.service.helper.ReuseUtils;
import com.vip.saturn.job.console.utils.SaturnConstants;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.annotation.Resource;
import org.apache.commons.io.FileUtils;

/**
 * @author hebelala
 */
public class ZkTreeServiceImpl implements ZkTreeService {

	private Gson gson = new Gson();
	private Random random = new Random();

	@Resource
	private CuratorRepository curatorRepository;

	@Resource
	private RegistryCenterService registryCenterService;

	@Override
	public ZkTree getZkTreeByNamespaceOfSession() throws SaturnJobConsoleException {
		try {
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
			String namespace = curatorFrameworkOp.getCuratorFramework().getNamespace();
			return getZkTree("/", namespace, curatorFrameworkOp);
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}
	}

	private ZkTree getZkTree(String path, String name, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		ZkTree zkTree = new ZkTree();
		zkTree.setName(name);
		zkTree.setData(curatorFrameworkOp.getData(path));
		zkTree.setStat(curatorFrameworkOp.getStat(path));
		List<String> children = curatorFrameworkOp.getChildren(path);
		if (children != null && !children.isEmpty()) {
			// Be care, the list must be mutable for sorting.
			Collections.sort(children);
			for (String child : children) {
				ZkTree childZkTree = getZkTree(path + "/" + child, child, curatorFrameworkOp);
				zkTree.getChildren().add(childZkTree);
			}
		}
		return zkTree;
	}

	@Override
	public ZkTree getZkTreeByNamespace(final String namespace) throws SaturnJobConsoleException {
		return ReuseUtils.reuse(namespace, registryCenterService, curatorRepository, new ReuseCallBack<ZkTree>() {
			@Override
			public ZkTree call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
					throws SaturnJobConsoleException {
				try {
					return getZkTree("/", namespace, curatorFrameworkOp);
				} catch (Exception e) {
					throw new SaturnJobConsoleException(e);
				}
			}
		});
	}

	@Override
	public ZkTree convertFileToZkTree(File file) throws SaturnJobConsoleException {
		try {
			String content = FileUtils.readFileToString(file, Constant.CHARSET_UTF8);
			return gson.fromJson(content, ZkTree.class);
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}
	}

	@Override
	public ZkTree convertInputStreamToZkTree(InputStream inputStream) throws SaturnJobConsoleException {
		try {
			return gson.fromJson(new InputStreamReader(inputStream, Constant.CHARSET_UTF8), ZkTree.class);
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}
	}

	@Override
	public File convertZkTreeToFile(ZkTree zkTree) throws SaturnJobConsoleException {
		try {
			File tmp = new File(SaturnConstants.CACHES_FILE_PATH,
					"tmp_zk_tree_" + System.currentTimeMillis() + "_" + random.nextInt(1000) + ".json");
			if (!tmp.exists()) {
				FileUtils.forceMkdir(tmp.getParentFile());
				tmp.createNewFile();
			}
			FileUtils.writeStringToFile(tmp, gson.toJson(zkTree), Constant.CHARSET_UTF8, false);
			return tmp;
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}
	}
}
