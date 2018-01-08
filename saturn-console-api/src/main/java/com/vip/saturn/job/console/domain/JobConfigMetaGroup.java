package com.vip.saturn.job.console.domain;

import java.util.List;
import java.util.Map;

public class JobConfigMetaGroup {

	private List<Map<String, String>> jobConfigMetas;

	public List<Map<String, String>> getJobConfigMetas() {
		return jobConfigMetas;
	}

	public void setJobConfigMetas(List<Map<String, String>> jobConfigMetas) {
		this.jobConfigMetas = jobConfigMetas;
	}
}
