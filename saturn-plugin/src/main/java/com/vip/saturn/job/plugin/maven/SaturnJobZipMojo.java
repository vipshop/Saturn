package com.vip.saturn.job.plugin.maven;

import java.io.File;
import java.net.URL;
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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import com.vip.saturn.job.plugin.maven.utils.IvyGetArtifact;
import com.vip.saturn.job.plugin.maven.utils.MavenProjectUtils;
import com.vip.saturn.job.plugin.utils.CommonUtils;

/**
 *
 * @author xiaopeng.he
 *
 */
@Mojo(name = "zip", requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class SaturnJobZipMojo extends AbstractMojo {

	@Component
    private MavenProjectHelper projectHelper;
	
	@SuppressWarnings({ "unchecked" })
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (!CommonUtils.initSaturnHome())
			throw new MojoExecutionException("The ${user.home}/.saturn/caches is not exists");

		Log log = getLog();

		MavenProject project = (MavenProject) getPluginContext().get("project");
		String version = getSaturnVersion(project);
		log.info("Packing the saturn job into a zip file: version:"+version);
		MavenProjectUtils mavenProjectUtils = new MavenProjectUtils(project, log);
		IvyGetArtifact ivyGetArtifact = getIvyGetArtifact(mavenProjectUtils);
		File saturnExecutorZip = getSaturnExecutorZip(ivyGetArtifact, version);

		File saturnHomeCaches = CommonUtils.getSaturnHomeCaches();
		try {
			CommonUtils.unzip(saturnExecutorZip, saturnHomeCaches);
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException("unzip saturn-executor.zip failed", e);
		}
		File saturnContainerDir = new File(saturnHomeCaches, "saturn-executor-" + version);

		List<File> runtimeLibFiles = new ArrayList<File>();
		List<Artifact> runtimeArtifacts = project.getRuntimeArtifacts();
		for (Artifact artifact : runtimeArtifacts) {
			runtimeLibFiles.add(artifact.getFile());
		}
		// Maybe could be more cool.
		runtimeLibFiles.add(new File(project.getBuild().getDirectory(),
				project.getArtifactId() + "-" + project.getVersion() + ".jar"));

		File zipFile = new File(project.getBuild().getDirectory(),
				project.getArtifactId() + "-" + project.getVersion() + "-" + "executor.zip");
		try {
			CommonUtils.zip(runtimeLibFiles, saturnContainerDir, zipFile);
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException("zip " + zipFile + " failed", e);
		}
		
		projectHelper.attachArtifact(project, "zip", "executor", zipFile);

	}


	private IvyGetArtifact getIvyGetArtifact(MavenProjectUtils mavenProjectUtils) throws MojoExecutionException {
		try {
			return mavenProjectUtils.getIvyGetArtifact();
		} catch (Exception e) {
			throw new MojoExecutionException("get IvyGetArtifact failed", e);
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
}
