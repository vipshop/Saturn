package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.integrate.entity.JobConfigInfo;
import com.vip.saturn.job.integrate.exception.UpdateJobConfigException;
import com.vip.saturn.job.integrate.service.UpdateJobConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author timmy.hu
 */
public class UpdateJobConfigServiceImpl implements UpdateJobConfigService {

	@Autowired
	private CurrentJobConfigService currentJobConfigService;

	@Override
	public void batchUpdatePreferList(List<JobConfigInfo> jobConfigInfos) throws UpdateJobConfigException {
		if (CollectionUtils.isEmpty(jobConfigInfos)) {
			return;
		}
		List<JobConfig4DB> currentJobConfigs = new ArrayList<JobConfig4DB>();
		JobConfig4DB currentJobConfig = null;
		for (JobConfigInfo jobConfigInfo : jobConfigInfos) {
			currentJobConfig = new JobConfig4DB();
			currentJobConfig.setNamespace(jobConfigInfo.getNamespace());
			currentJobConfig.setJobName(jobConfigInfo.getJobName());
			currentJobConfig.setPreferList(jobConfigInfo.getPerferList());
			currentJobConfigs.add(currentJobConfig);
		}
		try {
			currentJobConfigService.batchUpdatePreferList(currentJobConfigs);
		} catch (SaturnJobConsoleException e) {
			throw new UpdateJobConfigException(e);
		}
	}

}
