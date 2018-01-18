package com.vip.saturn.job.console.domain;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 作业配置页面的VO
 *
 * @author hebelala
 */
public class JobConfigVo extends JobConfig {

	private static final long serialVersionUID = 1L;

	private List<String> timeZonesProvided;
	private List<ExecutorProvided> preferListProvided;
	private List<String> dependenciesProvided;
	private JobStatus status;
	private List<String> pausePeriodDateList;
	private List<String> pausePeriodTimeList;
	private List<String> preferListList;
	private Boolean onlyUsePreferList;
	private List<String> dependenciesList;

	public void toVo() {
		pausePeriodDateList = toList(getPausePeriodDate());
		pausePeriodTimeList = toList(getPausePeriodTime());
		preferListList = toList(getPreferList());
		Boolean useDispreferList = getUseDispreferList();
		if (useDispreferList != null) {
			onlyUsePreferList = !useDispreferList;
		}
		dependenciesList = toList(getDependencies());
	}

	private List<String> toList(String str) {
		if (str == null) {
			return null;
		}
		List<String> list = new ArrayList<>();
		String[] split = str.split(",");
		for (String temp : split) {
			if (StringUtils.isNotBlank(temp)) {
				list.add(temp);
			}
		}
		return list;
	}

	private String fromList(List<String> list) {
		if (list == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0, size = list.size(); i < size; i++) {
			String element = list.get(i);
			if (StringUtils.isNotBlank(element)) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(element);
			}
		}
		return sb.toString();
	}

	public void fromVo() {
		setPausePeriodDate(fromList(pausePeriodDateList));
		setPausePeriodTime(fromList(pausePeriodTimeList));
		setPreferList(fromList(preferListList));
		if (onlyUsePreferList != null) {
			setUseDispreferList(!onlyUsePreferList);
		}
		setDependencies(fromList(dependenciesList));
	}

	public List<String> getTimeZonesProvided() {
		return timeZonesProvided;
	}

	public void setTimeZonesProvided(List<String> timeZonesProvided) {
		this.timeZonesProvided = timeZonesProvided;
	}

	public List<ExecutorProvided> getPreferListProvided() {
		return preferListProvided;
	}

	public void setPreferListProvided(List<ExecutorProvided> preferListProvided) {
		this.preferListProvided = preferListProvided;
	}

	public List<String> getDependenciesProvided() {
		return dependenciesProvided;
	}

	public void setDependenciesProvided(List<String> dependenciesProvided) {
		this.dependenciesProvided = dependenciesProvided;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public List<String> getPausePeriodDateList() {
		return pausePeriodDateList;
	}

	public void setPausePeriodDateList(List<String> pausePeriodDateList) {
		this.pausePeriodDateList = pausePeriodDateList;
	}

	public List<String> getPausePeriodTimeList() {
		return pausePeriodTimeList;
	}

	public void setPausePeriodTimeList(List<String> pausePeriodTimeList) {
		this.pausePeriodTimeList = pausePeriodTimeList;
	}

	public List<String> getPreferListList() {
		return preferListList;
	}

	public void setPreferListList(List<String> preferListList) {
		this.preferListList = preferListList;
	}

	public Boolean getOnlyUsePreferList() {
		return onlyUsePreferList;
	}

	public void setOnlyUsePreferList(Boolean onlyUsePreferList) {
		this.onlyUsePreferList = onlyUsePreferList;
	}

	public List<String> getDependenciesList() {
		return dependenciesList;
	}

	public void setDependenciesList(List<String> dependenciesList) {
		this.dependenciesList = dependenciesList;
	}

}
