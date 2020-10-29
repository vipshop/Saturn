/**
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */
package com.vip.saturn.job.console.service.helper;

import com.vip.saturn.job.console.domain.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.springframework.util.CollectionUtils;

public class DashboardServiceHelper {

	public static List<DomainStatistics> sortDomainByAllTimeFailureRate(List<DomainStatistics> domainList) {
		Collections.sort(domainList, new Comparator<DomainStatistics>() {
			@Override
			public int compare(DomainStatistics o1, DomainStatistics o2) {
				return Float.compare(o2.getFailureRateOfAllTime(), o1.getFailureRateOfAllTime());
			}
		});
		return domainList;
	}

	public static List<DomainStatistics> sortDomainByShardingCount(List<DomainStatistics> domainList) {
		Collections.sort(domainList, new Comparator<DomainStatistics>() {
			@Override
			public int compare(DomainStatistics o1, DomainStatistics o2) {
				return o1.getShardingCount() < o2.getShardingCount() ? 1
						: (o1.getShardingCount() == o2.getShardingCount() ? 0 : -1);
			}
		});
		return domainList;
	}

	public static List<JobStatistics> sortJobByAllTimeFailureRate(List<JobStatistics> jobList) {
		Collections.sort(jobList, new Comparator<JobStatistics>() {
			@Override
			public int compare(JobStatistics o1, JobStatistics o2) {
				return Float.compare(o2.getFailureRateOfAllTime(), o1.getFailureRateOfAllTime());
			}
		});
		return jobList;
	}

	public static List<JobStatistics> sortJobByLoadLevel(List<JobStatistics> jobList) {
		Collections.sort(jobList, new Comparator<JobStatistics>() {
			@Override
			public int compare(JobStatistics o1, JobStatistics o2) {
				return o1.getTotalLoadLevel() < o2.getTotalLoadLevel() ? 1
						: (o1.getTotalLoadLevel() == o2.getTotalLoadLevel() ? 0 : -1);
			}
		});
		return jobList;
	}

	public static List<JobStatistics> sortJobByDayProcessCount(List<JobStatistics> jobList) {
		Collections.sort(jobList, new Comparator<JobStatistics>() {
			@Override
			public int compare(JobStatistics o1, JobStatistics o2) {
				return o1.getProcessCountOfTheDay() < o2.getProcessCountOfTheDay() ? 1
						: (o1.getProcessCountOfTheDay() == o2.getProcessCountOfTheDay() ? 0 : -1);
			}
		});
		return jobList;
	}

	public static List<ExecutorStatistics> sortExecutorByLoadLevel(List<ExecutorStatistics> executorList) {
		Collections.sort(executorList, new Comparator<ExecutorStatistics>() {
			@Override
			public int compare(ExecutorStatistics o1, ExecutorStatistics o2) {
				return o1.getLoadLevel() < o2.getLoadLevel() ? 1 : (o1.getLoadLevel() == o2.getLoadLevel() ? 0 : -1);
			}
		});
		return executorList;
	}

	public static List<AbnormalJob> sortUnnormaoJobByTimeDesc(List<AbnormalJob> unnormalJobList) {
		Collections.sort(unnormalJobList, new Comparator<AbnormalJob>() {
			@Override
			public int compare(AbnormalJob o1, AbnormalJob o2) {
				return o1.getNextFireTime() < o2.getNextFireTime() ? 1
						: (o1.getNextFireTime() == o2.getNextFireTime() ? 0 : -1);
			}
		});
		return unnormalJobList;
	}

	public static List<ExecutorStatistics> sortExecutorByFailureRate(List<ExecutorStatistics> executorList) {
		Collections.sort(executorList, new Comparator<ExecutorStatistics>() {
			@Override
			public int compare(ExecutorStatistics o1, ExecutorStatistics o2) {
				return Float.compare(o2.getFailureRateOfTheDay(), o1.getFailureRateOfTheDay());
			}
		});
		return executorList;
	}

	public static AbnormalJob findEqualAbnormalJob(AbnormalJob example, List<AbnormalJob> oldUnnormalJobList) {
		if (CollectionUtils.isEmpty(oldUnnormalJobList)) {
			return null;
		}
		for (AbnormalJob oldUnnormalJob : oldUnnormalJobList) {
			if (oldUnnormalJob.equals(example)) {
				return oldUnnormalJob;
			}
		}
		return null;
	}

	public static Timeout4AlarmJob findEqualTimeout4AlarmJob(Timeout4AlarmJob example,
			List<Timeout4AlarmJob> oldTimeout4AlarmJobList) {
		if (CollectionUtils.isEmpty(oldTimeout4AlarmJobList)) {
			return null;
		}
		for (Timeout4AlarmJob timeout4AlarmJob : oldTimeout4AlarmJobList) {
			if (timeout4AlarmJob.equals(example)) {
				return timeout4AlarmJob;
			}
		}
		return null;
	}

	public static DisabledTimeoutAlarmJob findEqualDisabledTimeoutJob(DisabledTimeoutAlarmJob example,
			List<DisabledTimeoutAlarmJob> oldDisabledTimeoutJobList) {
		if (CollectionUtils.isEmpty(oldDisabledTimeoutJobList)) {
			return null;
		}
		for (DisabledTimeoutAlarmJob disabledTimeoutAlarmJob : oldDisabledTimeoutJobList) {
			if (disabledTimeoutAlarmJob.equals(example)) {
				return disabledTimeoutAlarmJob;
			}
		}
		return null;
	}

}
