package com.vip.saturn.job.plugin.maven.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.text.ParseException;

import org.apache.ivy.Ivy;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * 
 * @author xiaopeng.he
 *
 */
public class MavenProjectUtils {

	private final Log logger;

	private final MavenProject mavenProject;

	private final String localRepository;

	private final String fileSeparator = System.getProperty("file.separator");
	private final String userHome = System.getProperty("user.home");
	private final String ivyCache = userHome + fileSeparator + ".ivy_cache";
	private final String defaultM2Repository = userHome + fileSeparator + ".m2" + fileSeparator + "repository";

	public MavenProjectUtils(MavenProject mavenProject, Log log) {
		this.logger = log;
		this.mavenProject = mavenProject;
		this.localRepository = getLocalRepository0();
	}

	public String getLocalRepository() {
		return localRepository;
	}

	public IvyGetArtifact getIvyGetArtifact() throws ParseException, IOException {
		File cacheDirectory = new File(ivyCache);// ivy下包缓存目录
		if (!cacheDirectory.exists()) {
			cacheDirectory.mkdirs();
		}
		URL settingsURL = MavenProjectUtils.class.getClassLoader().getResource("ivysettings.xml");
		Ivy ivy = Ivy.newInstance();
		ivy.getSettings().setDefaultCache(cacheDirectory);
		ivy.getSettings().setVariable("ivy.local.default.root", localRepository);
		ivy.getSettings().load(settingsURL);
		return new IvyGetArtifact(logger, ivy);
	}

	/**
	 * Get local maven repo location
	 */
	private String getLocalRepository0() {
		String localRepositoryDir = null;
		try {
			Field projectBuilderConfiguration = mavenProject.getClass().getDeclaredField("projectBuilderConfiguration");
			projectBuilderConfiguration.setAccessible(true);
			Object projectBuilderConfiguration_object = projectBuilderConfiguration.get(mavenProject);// org.apache.maven.project.DefaultProjectBuildingRequest

			Field localRepository = projectBuilderConfiguration_object.getClass().getDeclaredField("localRepository");
			localRepository.setAccessible(true);
			Object localRepository_object = localRepository.get(projectBuilderConfiguration_object);

			Object basedirParent = null;
			try {
				Field userLocalArtifactRepository = localRepository_object.getClass()
						.getDeclaredField("userLocalArtifactRepository");// for maven 3.1.1
				userLocalArtifactRepository.setAccessible(true);
				basedirParent = userLocalArtifactRepository.get(localRepository_object);
			} catch (Exception e) {
				basedirParent = localRepository_object;// for maven 3.2.3
			}

			Field basedir = basedirParent.getClass().getDeclaredField("basedir");
			basedir.setAccessible(true);
			localRepositoryDir = (String) basedir.get(basedirParent);
		} catch (Exception e) {
			logger.warn("Maven's version is not suitable, use the default local maven repository");
		}
		return localRepositoryDir == null ? defaultM2Repository : localRepositoryDir;// default local maven dir
	}

}
