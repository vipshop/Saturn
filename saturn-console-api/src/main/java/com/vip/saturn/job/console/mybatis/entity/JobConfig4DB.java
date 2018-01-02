package com.vip.saturn.job.console.mybatis.entity;

import com.vip.saturn.job.console.domain.JobConfig;

import java.util.Date;

/**
 * @author hebelala
 */
public class JobConfig4DB extends JobConfig {

	private static final long serialVersionUID = 1L;

	private Integer rownum;

	private Long id;

	private String createBy;

	private String lastUpdateBy;

	private Date createTime;

	private Date lastUpdateTime;

	private String namespace;

	private String zkList;

	public Integer getRownum() {
		return rownum;
	}

	public void setRownum(Integer rownum) {
		this.rownum = rownum;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}

	public String getLastUpdateBy() {
		return lastUpdateBy;
	}

	public void setLastUpdateBy(String lastUpdateBy) {
		this.lastUpdateBy = lastUpdateBy;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(Date lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getZkList() {
		return zkList;
	}

	public void setZkList(String zkList) {
		this.zkList = zkList;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}

		JobConfig4DB that = (JobConfig4DB) o;

		if (rownum != null ? !rownum.equals(that.rownum) : that.rownum != null) {
			return false;
		}
		if (id != null ? !id.equals(that.id) : that.id != null) {
			return false;
		}
		if (createBy != null ? !createBy.equals(that.createBy) : that.createBy != null) {
			return false;
		}
		if (lastUpdateBy != null ? !lastUpdateBy.equals(that.lastUpdateBy) : that.lastUpdateBy != null) {
			return false;
		}
		if (createTime != null ? !createTime.equals(that.createTime) : that.createTime != null) {
			return false;
		}
		if (lastUpdateTime != null ? !lastUpdateTime.equals(that.lastUpdateTime) : that.lastUpdateTime != null) {
			return false;
		}
		if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) {
			return false;
		}
		return zkList != null ? zkList.equals(that.zkList) : that.zkList == null;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (rownum != null ? rownum.hashCode() : 0);
		result = 31 * result + (id != null ? id.hashCode() : 0);
		result = 31 * result + (createBy != null ? createBy.hashCode() : 0);
		result = 31 * result + (lastUpdateBy != null ? lastUpdateBy.hashCode() : 0);
		result = 31 * result + (createTime != null ? createTime.hashCode() : 0);
		result = 31 * result + (lastUpdateTime != null ? lastUpdateTime.hashCode() : 0);
		result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
		result = 31 * result + (zkList != null ? zkList.hashCode() : 0);
		return result;
	}
}
