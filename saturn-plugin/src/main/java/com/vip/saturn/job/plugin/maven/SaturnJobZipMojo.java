package com.vip.saturn.job.plugin.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
		log.info("Packing the saturn job into a zip file: version:" + version);

		List<File> runtimeLibFiles = new ArrayList<File>();
		List<Artifact> runtimeArtifacts = project.getRuntimeArtifacts();
		for (Artifact artifact : runtimeArtifacts) {
			runtimeLibFiles.add(artifact.getFile());
		}
		runtimeLibFiles.add(new File(project.getBuild().getDirectory(),
				project.getBuild().getFinalName() + "." + project.getPackaging()));

		File zipFile = new File(project.getBuild().getDirectory(),
				project.getArtifactId() + "-" + project.getVersion() + "-" + "app.zip");
		try {
			CommonUtils.zip(runtimeLibFiles, null, zipFile);
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException("zip " + zipFile + " failed", e);
		}

		projectHelper.attachArtifact(project, "zip", "executor", zipFile);

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

}
