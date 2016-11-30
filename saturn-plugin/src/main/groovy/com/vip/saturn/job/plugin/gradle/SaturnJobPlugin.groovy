package com.vip.saturn.job.plugin.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Zip

import com.vip.saturn.job.plugin.utils.CommonUtils;

/**
 * 
 * @author xiaopeng.he
 *
 */
class SaturnJobPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {

		if(!CommonUtils.initSaturnHome()) throw new GradleException("The \${user.home}/.saturn/caches is not exists")
		
		project.extensions.create("saturnJob", SaturnJobPluginExtension)

		def saturnCaches = CommonUtils.getSaturnHomeCaches()

		project.tasks.create(name: "saturnJobZip", type: Zip) {
			group = "saturn-job"
			description = "Pack the saturn job into a zip file"
			if(!project.configurations.names.contains('saturnJobExecutor')) {
				project.configurations { saturnJobExecutor }
				project.dependencies.add('saturnJobExecutor', "com.vip.saturn:saturn-job-executor:${project.saturnJobVersion}:zip@zip")
			}
			def saturnJobExecutorZip
			project.configurations.saturnJobExecutor.each {
				if(it.name.startsWith("saturn-job-executor")) {
					saturnJobExecutorZip = it.absolutePath
				}
			}
			project.copy {
				from project.zipTree(saturnJobExecutorZip)
				into saturnCaches
			}
			baseName = project.name
			version = project.version
			classifier = 'executor'
			into("saturn", {
				from "$saturnCaches/saturn-job-executor-${project.saturnJobVersion}"
			})
			into("app/lib", {
				from project.configurations.runtime
				from project.configurations.runtime.allArtifacts.files
			})
		}.dependsOn("jar")

		def saturnJobRunTask = project.tasks.create(name: "saturnJobRun", type: JavaExec).doFirst {
			if(!project.configurations.names.contains('saturnJobExecutor')) {
				project.configurations { saturnJobExecutor }
				project.dependencies.add('saturnJobExecutor', "com.vip.saturn:saturn-job-executor:${project.saturnJobVersion}:zip@zip")
			}
			def saturnJobExecutorZip
			project.configurations.saturnJobExecutor.each {
				if(it.name.startsWith("saturn-job-executor")) {
					saturnJobExecutorZip = it.absolutePath
				}
			}
			project.copy {
				from project.zipTree(saturnJobExecutorZip)
				into saturnCaches
			}

			def saturnExecutorDir = new File(saturnCaches.absolutePath, "saturn-job-executor-" + project.saturnJobVersion).path

			classpath = project.files(new FileNameFinder().getFileNames(saturnExecutorDir, "**/saturn-job-executor.jar")[0])
			main = "com.vip.saturn.job.executor.Main"
			jvmArgs = jvmArgs + []
			if(project.hasProperty("saturnJob.debug") && Boolean.parseBoolean(project.property("saturnJob.debug"))) {
				def debugPort = project.hasProperty("saturnJob.debug.port") ? Integer.parse(project.property("saturnJob.debug.port")) : "6666"
				jvmArgs = jvmArgs + ['-Xdebug', "-Xrunjdwp:transport=dt_socket,address=$debugPort,server=y,suspend=n"]
			}
			def namespace
			if(project.hasProperty("namespace")) namespace = project.property("namespace")
			if(namespace != null) args = args + ["-namespace", namespace]
			def executorName
			if(project.hasProperty("executorName")) executorName = project.property("executorName")
			if(executorName != null) args = args + ["-executorName", executorName]

			def saturnLibDir = saturnExecutorDir + System.getProperty("file.separator") + "lib"
			args = args + ["-saturnLibDir", saturnLibDir]

			def appLibDir = project.buildDir.path + System.getProperty("file.separator") + "saturn-run"
			project.copy {
				from project.configurations.runtime + project.configurations.runtime.allArtifacts.files
				into appLibDir
			}

			args = args + ["-appLibDir", appLibDir]
		}.dependsOn("jar")
		saturnJobRunTask.group = "saturn-job"
		saturnJobRunTask.description = "Run the saturn job"
	}
}
