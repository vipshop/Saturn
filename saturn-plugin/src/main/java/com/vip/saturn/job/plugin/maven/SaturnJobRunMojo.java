package com.vip.saturn.job.plugin.maven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.vip.saturn.job.plugin.maven.utils.IvyGetArtifact;
import com.vip.saturn.job.plugin.maven.utils.MavenProjectUtils;
import com.vip.saturn.job.plugin.utils.CommonUtils;
import org.apache.maven.shared.utils.io.FileUtils;

/**
 *
 * @author xiaopeng.he
 *
 */
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.COMPILE)
public class SaturnJobRunMojo extends AbstractMojo {

	@Parameter(property = "namespace")
	private String namespace;

	@Parameter(property = "executorName")
	private String executorName;

	@SuppressWarnings("unchecked")
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (!CommonUtils.initSaturnHome())
			throw new MojoExecutionException("The ${user.home}/.saturn/caches is not exists");

		Log log = getLog();
		log.info("Running saturn job");

		final MavenProject project = (MavenProject) getPluginContext().get("project");

		// 拷贝应用运行时依赖至target/saturn-run目录
		File saturnAppLibDir = new File(
				project.getBuild().getDirectory() + System.getProperty("file.separator") + "saturn-run");
		if (!saturnAppLibDir.exists()) {
			saturnAppLibDir.mkdirs();
		}
		try {
			List<String> runtimeArtifacts = project.getRuntimeClasspathElements();
			for (String path : runtimeArtifacts) {
				File tmp = new File(path);
				copy(tmp, saturnAppLibDir);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException("getRuntimeClasspathElements error", e);
		}

		String saturnVersion = getSaturnVersion(project);
		MavenProjectUtils mavenProjectUtils = new MavenProjectUtils(project, log);
		IvyGetArtifact ivyGetArtifact = getIvyGetArtifact(mavenProjectUtils);

		File saturnExecutorZip = getSaturnExecutorZip(ivyGetArtifact, saturnVersion);

		File saturnHomeCaches = CommonUtils.getSaturnHomeCaches();
		try {
			CommonUtils.unzip(saturnExecutorZip, saturnHomeCaches);
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException("unzip saturn-executor.zip failed", e);
		}
		File saturnExecutorDir = new File(saturnHomeCaches, "saturn-executor-" + saturnVersion);

		final URLClassLoader executorClassLoader;
		try {
			executorClassLoader = new URLClassLoader(
					new URL[] { new File(saturnExecutorDir, "saturn-executor.jar").toURL() },
					ClassLoader.getSystemClassLoader());
		} catch (MalformedURLException e) {
			throw new MojoExecutionException("get saturn-executor classLoad failed", e);
		}

		final List<String> argList = new ArrayList<>();
		if (namespace != null) {
			argList.add("-namespace");
			argList.add(namespace);
		} else {
			throw new MojoExecutionException("the parameter of namespace is required");
		}
		if (executorName != null) {
			argList.add("-executorName");
			argList.add(executorName);
		}

		String saturnLibDir = saturnExecutorDir + System.getProperty("file.separator") + "lib";
		argList.add("-saturnLibDir");
		argList.add(saturnLibDir);

		argList.add("-appLibDir");
		argList.add(saturnAppLibDir.getAbsolutePath());

		Thread containerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.currentThread().setContextClassLoader(executorClassLoader);
					System.setProperty("saturn.stdout", "true");
					Class<?> mainClass = executorClassLoader.loadClass("com.vip.saturn.job.executor.Main");
					Method mainMethod = mainClass.getMethod("main", String[].class);
					mainMethod.invoke(mainClass, new Object[] { argList.toArray(new String[argList.size()]) });
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
		containerThread.start();

		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new MojoExecutionException("current thread join error", e);
		}

	}

	@SuppressWarnings("unchecked")
	private String getSaturnVersion(MavenProject project) throws MojoExecutionException {
		List<Artifact> artifacts = project.getRuntimeArtifacts();
		if (artifacts != null && !artifacts.isEmpty()) {
			for (int i = 0; i < artifacts.size(); i++) {
				Artifact artifact = artifacts.get(i);
				if ("saturn-job-api".equals(artifact.getArtifactId())) {
					return artifact.getBaseVersion();
				}
			}
		}
		throw new MojoExecutionException("cannot read the saturn-job-core dependency.");
	}

	private IvyGetArtifact getIvyGetArtifact(MavenProjectUtils mavenProjectUtils) throws MojoExecutionException {
		try {
			return mavenProjectUtils.getIvyGetArtifact();
		} catch (Exception e) {
			throw new MojoExecutionException("get IvyGetArtifact failed", e);
		}
	}

	private File getSaturnExecutorZip(IvyGetArtifact ivyGetArtifact, String saturnVersion)
			throws MojoExecutionException {
		Set<Map<String, Object>> executorArtifacts = new HashSet<Map<String, Object>>();
		Map<String, Object> executorZip = new HashMap<String, Object>();
		executorZip.put("name", "saturn-executor");
		executorZip.put("type", "zip");
		executorZip.put("ext", "zip");
		executorArtifacts.add(executorZip);
		Map<String, String> extraAttributes = new HashMap<String, String>();
		extraAttributes.put("m:classifier", "zip");
		executorZip.put("extraAttributes", extraAttributes);
		List<URL> executorURLs = null;
		try {
			executorURLs = ivyGetArtifact.get("com.vip.saturn", "saturn-executor", saturnVersion,
					new String[] { "master(*)" }, executorArtifacts);
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException("download saturn-executor failed", e);
		}
		if (executorURLs == null || executorURLs.isEmpty()) {
			throw new MojoExecutionException("download saturn-executor failed");
		}
		return new File(executorURLs.get(0).getFile());
	}

	private static List<URL> getUrls(File file) throws MalformedURLException {
		List<URL> urls = new ArrayList<>();
		if (file.exists()) {
			if (file.isDirectory()) {
				File[] files = file.listFiles();
				for (File tmp : files) {
					urls.addAll(getUrls(tmp));
				}
			} else if (file.isFile()) {
				urls.add(file.toURI().toURL());
			}
		}
		return urls;
	}

	private void copy(File source, File destinationDir) throws IOException {
		if (source.isFile()) {
			FileUtils.copyFileToDirectory(source, destinationDir);
		} else if (source.isDirectory()) {
			File newDestinationDir = new File(destinationDir, source.getName());
			if (!newDestinationDir.exists()) {
				newDestinationDir.mkdirs();
			}
			for (File tmp : source.listFiles()) {
				copy(tmp, newDestinationDir);
			}
		}
	}

}
