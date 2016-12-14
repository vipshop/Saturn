package com.vip.saturn.job.executor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author xiaopeng.he
 *
 */

public class Main {

	private String namespace;
	private int monitorPort = -1;
	private String executorName;
	
	private String saturnLibDir = getLibDir("lib");
	private String appLibDir = getLibDir("lib");
	
	private ClassLoader executorClassLoader;
	private ClassLoader jobClassLoader;

	private Object saturnExecutor;
	public Object getSaturnExecutor(){
		return saturnExecutor;
	}
	protected void parseArgs(String[] inArgs) throws Exception {
		String[] args = inArgs.clone();
		
		for(int i=0; i<args.length; i++) {
			String param = args[i].trim();
			if("-namespace".equals(param)) {
				try {
					this.namespace = args[++i].trim();//NOSONAR
					System.setProperty("app.instance.name", this.namespace); // For logback.
					System.setProperty("namespace", this.namespace); // For logback.
				} catch(Exception e) {
					throw new Exception("Please set namespace value, exception message: " + e.getMessage(),e);
				}
			} else if("-monport".equals(param)) {
				try {
					monitorPort = Integer.parseInt(args[++i].trim());//NOSONAR
				} catch (Exception e) {
					throw new Exception("Please set monitor port value, exception message: " + e.getMessage(),e);
				}
			} else if("-executorName".equals(param)) {
				//String executorName;
				try {
					executorName = args[++i].trim();//NOSONAR
				} catch (Exception e) {
					throw new Exception("Please set executorName value, exception message: " + e.getMessage(),e);
				}
				//this.executorName = executorName;
			} else if("-saturnLibDir".equals(param)) {
				//String saturnLibDir;
				try {
					saturnLibDir = args[++i].trim();//NOSONAR
				} catch (Exception e) {
					throw new Exception("Please set saturnLibDir value, exception message: " + e.getMessage(),e);
				}
				//this.saturnLibDir = saturnLibDir;
			} else if("-appLibDir".equals(param)) {
				//String appLibDir;
				try {
					appLibDir = args[++i].trim();//NOSONAR
				} catch (Exception e) {
					throw new Exception("Please set appLibDir value, exception message: " + e.getMessage(),e);
				}
				//this.appLibDir = appLibDir;
			} 
		}		
		if(namespace == null) {
			throw new Exception("Please add the namespace parameter");
		}
	}

	public String getNamespace() {
		return namespace;
	}


	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getExecutorName() {
		if(saturnExecutor != null) {
			try {
				return (String) saturnExecutor.getClass().getMethod("getExecutorName").invoke(saturnExecutor);
			} catch( Exception e) {//NOSONAR
			}
		}
		return executorName;
	}


	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}


	private static String getLibDir(String dirName) {
		File root = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile();
		File lib = new File(root, dirName);
		if(!lib.isDirectory()){
			return null;
		}
		return lib.getAbsolutePath();
	}
	

	private static List<URL> getUrls(File file) throws MalformedURLException {
		List<URL> urls = new ArrayList<>();
		if(!file.exists()) {
			return urls;
		}
		if(file.isDirectory()) {
			if("classes".equals(file.getName())){
				urls.add(file.toURI().toURL());
				return urls;
			}
			File[] files = file.listFiles();
			if(files != null && files.length >0){
				for(File tmp : files) {
					urls.addAll(getUrls(tmp));
				}
			}
			return urls;
		}
		if(file.isFile()) {
			urls.add(file.toURI().toURL());
		}
		return urls;
	}
	
	
	public void initClassLoader() throws Exception{
		List<URL> urls = getUrls(new File(saturnLibDir));	
		executorClassLoader = new SaturnClassLoader(urls.toArray(new URL[urls.size()]), Main.class.getClassLoader());//NOSONAR
		if(new File(appLibDir).isDirectory()){
			urls = getUrls(new File(appLibDir));	
			jobClassLoader = new JobClassLoader(urls.toArray(new URL[urls.size()]));//NOSONAR
		}else{
			jobClassLoader = executorClassLoader;
		}
	}

	public void startExecutor() throws Exception{
		Class<?> startExecutorClass = executorClassLoader.loadClass("com.vip.saturn.job.executor.SaturnExecutor");
		saturnExecutor = startExecutorClass.getMethod("buildExecutor", String.class, int.class, String.class).invoke(null, namespace, monitorPort, executorName);
		startExecutorClass.getMethod("execute", ClassLoader.class, ClassLoader.class).invoke(saturnExecutor, executorClassLoader, jobClassLoader);
	}
	
	public void launch(String[] args, ClassLoader jobClassLoader) throws Exception{
		parseArgs(args);
		initClassLoader();
		this.jobClassLoader = jobClassLoader;
		startExecutor();
	}
	
	public void launchInner(String[] args, ClassLoader saturnClassLoader, ClassLoader jobClassLoader) throws Exception{
		parseArgs(args);
		this.executorClassLoader = saturnClassLoader;
		this.jobClassLoader = jobClassLoader;
		startExecutor();
	}
	
	public void shutdown() throws Exception{
		Class<?> startExecutorClass = executorClassLoader.loadClass("com.vip.saturn.job.executor.SaturnExecutor");
		startExecutorClass.getMethod("shutdown").invoke(saturnExecutor);
	}
	
	public static void main(String[] args) {
		try {
			Main main = new Main();
			main.parseArgs(args);
			main.initClassLoader();
			main.startExecutor();
		} catch (Throwable t) {//NOSONAR
			t.printStackTrace();	//NOSONAR
			System.exit(1);
		}
	}
	
}