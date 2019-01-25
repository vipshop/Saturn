package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NamespaceServiceImplTest {

	@Mock
	private RegistryCenterService registryCenterService;

	@Mock
	private JobService jobService;

	@InjectMocks
	private NamespaceServiceImpl namespaceService;

	@Test
	public void testSrcNamespaceIsNull() throws SaturnJobConsoleException {
		SaturnJobConsoleHttpException exception = null;

		try {
			namespaceService.importJobsFromNamespaceToNamespace(null, null, null);
		} catch (SaturnJobConsoleHttpException e) {
			exception = e;
		}

		Assert.assertNotNull(exception);
		Assert.assertEquals(exception.getMessage(), "srcNamespace should not be null");
	}

	@Test
	public void testDestNamespaceIsNull() throws SaturnJobConsoleException {
		SaturnJobConsoleHttpException exception = null;
		try {

			namespaceService.importJobsFromNamespaceToNamespace("saturn.vip.vip.com", null, null);
		} catch (SaturnJobConsoleHttpException e) {
			exception = e;
		}

		Assert.assertNotNull(exception);
		Assert.assertEquals(exception.getMessage(), "destNamespace should not be null");
	}

	@Test
	public void testSrcNamespaceIdenticalToDestNamespace() throws SaturnJobConsoleException {
		SaturnJobConsoleHttpException exception = null;
		String srcNamespace = "saturn.vip.vip.com";
		String destNamespace = "saturn.vip.vip.com";

		try {
			namespaceService.importJobsFromNamespaceToNamespace(srcNamespace, destNamespace, null);
		} catch (SaturnJobConsoleHttpException e) {
			exception = e;
		}

		Assert.assertNotNull(exception);
		Assert.assertEquals(exception.getMessage(), "destNamespace and destNamespace should be difference");
	}

	@Test
	public void testNoJobsToImport() throws Exception {
		List<JobConfig> jobConfigs = new ArrayList<>();
		when(jobService.getUnSystemJobs("saturn.vip.vip.com")).thenReturn(jobConfigs);

		Map<String, List> result = namespaceService
				.importJobsFromNamespaceToNamespace("saturn.vip.vip.com", "saturn.vip.vip.com_tt", "ray.leung");
		Assert.assertThat(result.get("success").size(), is(0));
	}

	@Test
	public void testImportJobs() throws Exception {
		List<JobConfig> jobConfigs = new ArrayList<>();
		jobConfigs.add(new JobConfig());
		jobConfigs.add(new JobConfig());
		jobConfigs.add(new JobConfig());
		when(jobService.getUnSystemJobs("saturn.vip.vip.com")).thenReturn(jobConfigs);

		Map<String, List> result = namespaceService
				.importJobsFromNamespaceToNamespace("saturn.vip.vip.com", "saturn.vip.vip.com_tt", "ray.leung");

		Assert.assertThat(result.get("success").size(), is(3));
	}

	@Test
	public void testFailToImportJobs() throws Exception {
		List<JobConfig> jobConfigs = new ArrayList<>();
		jobConfigs.add(new JobConfig());
		when(jobService.getUnSystemJobs("saturn.vip.vip.com")).thenReturn(jobConfigs);
		doThrow(new RuntimeException()).when(jobService).addJob(anyString(), any(JobConfig.class), anyString());
		Exception exception = null;

		try {
			namespaceService
					.importJobsFromNamespaceToNamespace("saturn.vip.vip.com", "saturn.vip.vip.com_tt", "ray.leung");
		} catch (Exception e) {
			exception = e;
		}

		Assert.assertNotNull(exception);
	}
}