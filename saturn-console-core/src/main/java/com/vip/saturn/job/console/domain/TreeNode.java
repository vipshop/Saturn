/**
 * 
 */
package com.vip.saturn.job.console.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author chembo.huang
 *
 */
public class TreeNode {

	private String title;

	@JsonInclude(Include.NON_EMPTY)
	private String domain;

	@JsonInclude(Include.NON_EMPTY)
	private String fullPath;

	@JsonInclude(Include.NON_EMPTY)
	private String icon;

	@JsonInclude(Include.NON_EMPTY)
	private boolean lazy;

	@JsonInclude(Include.NON_EMPTY)
	private String extraClasses;

	private boolean folder = true;

	private int bid;

	@JsonInclude(Include.NON_EMPTY)
	private List<TreeNode> children = new ArrayList<>();

	public TreeNode() {
	}

	public TreeNode(String title) {
		this.title = title;
	}

	public String getTitle() {
		return this.title;
	}

	public String getDomain() {
		return this.domain;
	}

	public String getFullPath() {
		return this.fullPath;
	}

	public String getIcon() {
		return this.icon;
	}

	public boolean isLazy() {
		return this.lazy;
	}

	public String getExtraClasses() {
		return this.extraClasses;
	}

	public boolean isFolder() {
		return this.folder;
	}

	public int getBid() {
		return this.bid;
	}

	public List<TreeNode> getChildren() {
		return this.children;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public void setExtraClasses(String extraClasses) {
		this.extraClasses = extraClasses;
	}

	public void setFolder(boolean folder) {
		this.folder = folder;
	}

	public void setBid(int bid) {
		this.bid = bid;
	}

	public void setChildren(List<TreeNode> children) {
		this.children = children;
	}

	public String toString() {
		return "TreeNode(title=" + getTitle() + ", domain=" + getDomain() + ", fullPath=" + getFullPath() + ", icon="
				+ getIcon() + ", lazy=" + isLazy() + ", extraClasses=" + getExtraClasses() + ", folder=" + isFolder()
				+ ", bid=" + getBid() + ", children=" + getChildren() + ")";
	}

	public TreeNode deepCopy() {
		TreeNode result = new TreeNode();
		result.setBid(bid);
		result.setDomain(domain);
		result.setExtraClasses(extraClasses);
		result.setFolder(folder);
		result.setFullPath(fullPath);
		result.setIcon(icon);
		result.setLazy(lazy);
		result.setTitle(title);
		List<TreeNode> childs = new ArrayList<TreeNode>();
		TreeNode child = null;
		for (TreeNode thisChild : this.getChildren()) {
			child = thisChild.deepCopy();
			childs.add(child);
		}
		result.setChildren(childs);
		return result;
	}
}
