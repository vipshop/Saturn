package com.vip.saturn.job.executor;

import java.net.URL;
import java.net.URLClassLoader;

public class SaturnClassLoader extends URLClassLoader {
	public SaturnClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if(name == null){
			return null;
		}
		name = name.trim();
		if(name.isEmpty()){
			return null;
		}
		synchronized (getClassLoadingLock(name)) {
			Class<?> findClass = findLoadedClass(name);
			
			if (findClass == null) {
				findClass = super.loadClass(name, resolve);
			}
			return findClass;
		}
	}
}
