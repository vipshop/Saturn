package com.vip.saturn.job.console.domain;

/**
 * @author hebelala
 */
public class JobListElementVo extends JobConfig {

	private static final long serialVersionUID = 1L;

	private JobStatus status;

	private String shardingList;

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public String getShardingList() {
		return shardingList;
	}

	public void setShardingList(String shardingList) {
		this.shardingList = shardingList;
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

		JobListElementVo that = (JobListElementVo) o;

		if (status != that.status) {
			return false;
		}
		return shardingList != null ? shardingList.equals(that.shardingList) : that.shardingList == null;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (status != null ? status.hashCode() : 0);
		result = 31 * result + (shardingList != null ? shardingList.hashCode() : 0);
		return result;
	}
}
