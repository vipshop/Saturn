package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.JobDiffInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import java.util.List;

public interface ZkDBDiffService {

	/**
	 * Diff the config data in zk and db of the same zk cluster.
	 *
	 * @param clusterKey zk cluster key.
	 * @return The different info organized by job.
	 */
	List<JobDiffInfo> diffByCluster(String clusterKey) throws SaturnJobConsoleException;

	/**
	 * Diff the config data in zk and db of namespace.
	 *
	 * @return The different info organized by job.
	 */
	List<JobDiffInfo> diffByNamespace(String namespace) throws SaturnJobConsoleException;

	/**
	 * Diff the config data in zk and db of job.
	 *
	 * @return The different info organized by job. If no difference, return null;
	 */
	JobDiffInfo diffByJob(String namespace, String jobName) throws SaturnJobConsoleException;
}
