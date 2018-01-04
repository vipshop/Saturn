package com.vip.saturn.job.console.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;

/**
 * @author hebelala
 */
public class SaturnBeanUtils {

	public static void copyProperties(Object source, Object target) {
		BeanUtils.copyProperties(source, target);
	}

	public static void copyPropertiesIgnoreNull(Object source, Object target) {
		final BeanWrapper beanWrapper = new BeanWrapperImpl(source);
		PropertyDescriptor[] pds = beanWrapper.getPropertyDescriptors();
		Set<String> names = new HashSet<>();
		for (PropertyDescriptor pd : pds) {
			Object value = beanWrapper.getPropertyValue(pd.getName());
			if (value == null) {
				names.add(pd.getName());
			}
		}
		BeanUtils.copyProperties(source, target, names.toArray(new String[names.size()]));
	}

}
