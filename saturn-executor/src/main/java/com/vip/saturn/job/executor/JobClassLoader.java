package com.vip.saturn.job.executor;

import java.net.URL;
import java.net.URLClassLoader;

public class JobClassLoader extends URLClassLoader {

	public JobClassLoader(URL[] urls) {
		super(urls, null);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (name == null) {
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
