package com.vip.saturn.job.console.domain.container;

/**
 * @author hebelala
 */
public class ContainerScaleJob {

	private ContainerScaleJobConfig containerScaleJobConfig;
	private Boolean enabled;

	public ContainerScaleJobConfig getContainerScaleJobConfig() {
		return containerScaleJobConfig;
	}

	public void setContainerScaleJobConfig(ContainerScaleJobConfig containerScaleJobConfig) {
		this.containerScaleJobConfig = containerScaleJobConfig;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
}
