package com.vip.saturn.job.plugin.maven.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.util.filter.FilterHelper;
import org.apache.maven.plugin.logging.Log;

/**
 * 
 * Get artifact by ivy
 * 
 * @author xiaopeng.he
 *
 */
public class IvyGetArtifact {

	private Log log;

	private Ivy ivy;

	public IvyGetArtifact(Log log, Ivy ivy) {
		this.log = log;
		this.ivy = ivy;
	}

	private File getIvyfile(String org, String name, String rev, String[] confs, Set<Map<String, Object>> artifacts)
			throws IOException {
		File ivyfile;
		ivyfile = File.createTempFile("ivy", ".xml");
		ivyfile.deleteOnExit();
		DefaultModuleDescriptor md = DefaultModuleDescriptor
				.newDefaultInstance(ModuleRevisionId.newInstance(org, name + "-caller", "working"));
		DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md,
				ModuleRevisionId.newInstance(org, name, rev), false, false, true);

		if (artifacts != null && !artifacts.isEmpty()) {
			for (Map<String, Object> artifact : artifacts) {
				String artifactName = (String) artifact.get("name");
				String artifactType = (String) artifact.get("type");
				String artifactExt = (String) artifact.get("ext");
				URL artifactUrl = (URL) artifact.get("url");
				Map<?, ?> extraAttributes = (Map<?, ?>) artifact.get("extraAttributes");
				DefaultDependencyArtifactDescriptor dad = new DefaultDependencyArtifactDescriptor(dd, artifactName,
						artifactType, artifactExt, artifactUrl, extraAttributes);
				dd.addDependencyArtifact("default", dad);
			}
		}

		for (int i = 0; i < confs.length; i++) {
			dd.addDependencyConfiguration("default", confs[i]);
		}
		md.addDependency(dd);

		md.addExtraAttributeNamespace("m", "http://ant.apache.org/ivy/maven");

		XmlModuleDescriptorWriter.write(md, ivyfile);
		return ivyfile;
	}

	private Set<URL> getCachePath(ModuleDescriptor md, String[] confs) throws ParseException, IOException {
		Set<URL> fs = new HashSet<URL>();
		StringBuffer buf = new StringBuffer();
		Collection<ArtifactDownloadReport> all = new LinkedHashSet<ArtifactDownloadReport>();
		ResolutionCacheManager cacheMgr = ivy.getResolutionCacheManager();
		XmlReportParser parser = new XmlReportParser();
		for (int i = 0; i < confs.length; i++) {
			String resolveId = ResolveOptions.getDefaultResolveId(md);
			File report = cacheMgr.getConfigurationResolveReportInCache(resolveId, confs[i]);
			parser.parse(report);
			all.addAll(Arrays.asList(parser.getArtifactReports()));
		}
		for (ArtifactDownloadReport artifact : all) {
			if (artifact.getLocalFile() != null) {
				buf.append(artifact.getLocalFile().getCanonicalPath());
				buf.append(File.pathSeparator);
			}
		}
		String[] fs_str = buf.toString().split(File.pathSeparator);
		for (String str : fs_str) {
			File file = new File(str);
			if (file.exists()) {
				fs.add(file.toURI().toURL());
			}
		}
		return fs;
	}

	public List<URL> get(String org, String name, String rev, String[] confs, Set<Map<String, Object>> artifacts)
			throws IOException, ParseException {
		Set<URL> artifactsGeted = new HashSet<URL>();
		try {
			ivy.getSettings().addAllVariables(System.getProperties());
			ivy.pushContext();
			File ivyfile = getIvyfile(org, name, rev, confs, artifacts);
			String[] conf2 = new String[] { "default" };
			ResolveOptions resolveOptions = new ResolveOptions().setConfs(conf2).setValidate(true).setResolveMode(null)
					.setArtifactFilter(FilterHelper.getArtifactTypeFilter("jar,bundle,zip"));
			ResolveReport report = ivy.resolve(ivyfile.toURI().toURL(), resolveOptions);
			if (report.hasError()) {
				List<?> problemMessages = report.getAllProblemMessages();
				for (Object message : problemMessages) {
					log.error(message.toString());
				}
			} else {
				artifactsGeted.addAll(getCachePath(report.getModuleDescriptor(), conf2));
			}
		} catch (IOException e) {
			throw e;
		} catch (ParseException e) {
			throw e;
		} finally {
			ivy.popContext();
		}
		List<URL> result = new ArrayList<URL>();
		result.addAll(artifactsGeted);
		return result;
	}

}
