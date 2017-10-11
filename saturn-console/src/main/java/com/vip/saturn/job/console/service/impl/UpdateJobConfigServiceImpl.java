/**
 * 
 */
package com.vip.saturn.job.console.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.integrate.entity.JobConfigInfo;
import com.vip.saturn.job.integrate.service.UpdateJobConfigService;

/**
 * @author timmy.hu
 *
 */
@Service
public class UpdateJobConfigServiceImpl implements UpdateJobConfigService {

	@Autowired
	private CurrentJobConfigService currentJobConfigService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.vip.saturn.job.integrate.service.UpdateJobConfigService#batchUpdatePerferList(java.util.List)
	 */
	@Override
	public void batchUpdatePerferList(List<JobConfigInfo> jobConfigInfos) {
		if (CollectionUtils.isEmpty(jobConfigInfos)) {
			return;
		}
		List<CurrentJobConfig> currentJobConfigs = new ArrayList<CurrentJobConfig>();
		CurrentJobConfig currentJobConfig = null;
		for (JobConfigInfo jobConfigInfo : jobConfigInfos) {
			currentJobConfig = new CurrentJobConfig();
			currentJobConfig.setNamespace(jobConfigInfo.getNamespace());
			currentJobConfig.setJobName(jobConfigInfo.getJobName());
			currentJobConfig.setPreferList(jobConfigInfo.getPerferList());
			currentJobConfigs.add(currentJobConfig);
		}
		currentJobConfigService.batchUpdatePerferList(currentJobConfigs);
	}

}
