/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.TreeNode;

/**
 * @author chembo.huang
 *
 */
public final class InitRegistryCenterService {
	private static final Logger log = LoggerFactory.getLogger(InitRegistryCenterService.class);
	public static TreeNode treeData = new TreeNode();
	public static final Map<String/** zkBsKey **/
			, TreeNode> ZKBSKEY_TO_TREENODE_MAP = new LinkedHashMap<>();
	public static TreeNode DOMAIN_ROOT_TREE_NODE = new TreeNode();
	private static AtomicBoolean domainTreeinited = new AtomicBoolean(false);

	/**
	 * transfer /a/b/b1, /a/b/b2 to {"title":"a", "children":["title":"b", "children": ["title": b1, "title": b2]]}
	 * 
	 * @param registryCenterConfiguration 注册中心配置类
	 */
	public static void initTreeJson(Set<RegistryCenterConfiguration> registryCenterConfiguration) {
		TreeNode treeData = new TreeNode();
		for (RegistryCenterConfiguration conf : registryCenterConfiguration) {
			String nameAndnameSpace = conf.getNameAndNamespace();
			if (nameAndnameSpace.startsWith("/")) {
				nameAndnameSpace = nameAndnameSpace.substring(1);
			}
			treeData = parseDirectory2Tree(nameAndnameSpace, treeData, conf.getNameAndNamespace(), conf.getDegree());
		}
		log.info("init tree data: {}", treeData);
	}

	public static void initTreeJson(ArrayList<RegistryCenterConfiguration> registryCenterConfiguration, String zkBsk) {
		TreeNode treeData = new TreeNode();
		for (RegistryCenterConfiguration conf : registryCenterConfiguration) {
			String nameAndnameSpace = conf.getNameAndNamespace();
			if (nameAndnameSpace.startsWith("/")) {
				nameAndnameSpace = nameAndnameSpace.substring(1);
			}
			treeData = parseDirectory2Tree(nameAndnameSpace, treeData, conf.getNameAndNamespace(), conf.getDegree());
		}
		ZKBSKEY_TO_TREENODE_MAP.put(zkBsk, treeData);
		log.info("init {} tree data: {}", zkBsk, treeData);
	}

	public static TreeNode getDomainRootTreeNode() {
		while (!domainTreeinited.get()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}
		return DOMAIN_ROOT_TREE_NODE;
	}

	public static void reloadDomainRootTreeNode() {
		TreeNode resultNode = null;
		for (TreeNode treeNode : InitRegistryCenterService.ZKBSKEY_TO_TREENODE_MAP.values()) {
			if (treeNode != null) {
				treeNode = treeNode.deepCopy();
				if (resultNode == null) {
					resultNode = treeNode;
				} else {
					for (TreeNode childNode : treeNode.getChildren()) {
						addToTree(resultNode, childNode);
					}
				}
			}
		}
		if (resultNode != null) {
			DOMAIN_ROOT_TREE_NODE = resultNode;
		}
		domainTreeinited.compareAndSet(false, true);
	}

	private static void addToTree(TreeNode parentNode, TreeNode childNode) {
		if (parentNode.getChildren() != null && parentNode.getChildren().size() == 0) {
			parentNode.getChildren().add(childNode);
			return;
		}
		TreeNode equalNode = null;
		for (TreeNode tn : parentNode.getChildren()) {
			if (childNode.getTitle().equals(tn.getTitle())) {
				equalNode = tn;
				break;
			}
		}
		if (equalNode != null) {
			for (TreeNode childTn : childNode.getChildren()) {
				addToTree(equalNode, childTn);
			}
		} else {
			parentNode.getChildren().add(childNode);
		}
	}

	/**
	 * generate a tree recursively.
	 * @param directory directory
	 * @param treeNodeParent treeNodeParent
	 * @param fullPath fullPath
	 * @return TreeNode
	 */
	public static TreeNode parseDirectory2Tree(String directory, TreeNode treeNodeParent, String fullPath,
			String degree) {
		String[] nodes = directory.split("/");
		String nodeName = nodes[0];
		TreeNode node = new TreeNode();
		node.setTitle(nodeName);
		// if there is only one node left, just saves it to the subs and return.
		if (nodes.length == 1) {
			node.setTitle(
					"<a title=\"总览\" href=overview?name=" + fullPath + " target=\"contentFrame\">" + nodeName + "</a>");
			node.setFullPath(fullPath);
			node.setDomain(nodeName);
			node.setFolder(false);
			node.setLazy(true);
			node.setExtraClasses("custom-degree-circle degree-" + degree);
			treeNodeParent.getChildren().add(node);
			return treeNodeParent;
		} else {
			directory = directory.replace(nodeName + "/", "");
			// check the first node, see if the tree already contains this parent node.
			// if not, create new node, otherwise, pass the exist node to next loop.
			if (treeNodeParent.getTitle() == null && !treeNodeParent.getChildren().isEmpty()) {
				List<TreeNode> listSubTree = treeNodeParent.getChildren();
				for (TreeNode tn : listSubTree) {
					if (nodeName.equals(tn.getTitle())) {
						parseDirectory2Tree(directory, tn, fullPath, degree);
						return treeNodeParent;
					}
				}
				treeNodeParent.getChildren().add(parseDirectory2Tree(directory, node, fullPath, degree));
				return treeNodeParent;
			}

			if (nodeName.equals(treeNodeParent.getTitle())) {
				parseDirectory2Tree(directory, treeNodeParent, fullPath, degree);
				return treeNodeParent;
			}

			List<TreeNode> listSubTree = treeNodeParent.getChildren();
			for (TreeNode tn : listSubTree) {
				if (nodeName.equals(tn.getTitle())) {
					parseDirectory2Tree(directory, tn, fullPath, degree);
					return tn;
				}
			}
			treeNodeParent.getChildren().add(parseDirectory2Tree(directory, node, fullPath, degree));
			return treeNodeParent;
		}
	}

}
