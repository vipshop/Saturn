package com.vip.saturn.job.utils;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

public class SaturnUtilsTest {

	@Test
	public void getErrorMessage() {
		assertEquals("java.lang.RuntimeException: eeee", SaturnUtils.getErrorMessage(new RuntimeException("eeee")));
		assertEquals("java.lang.Error: eeee", SaturnUtils.getErrorMessage(new Error("eeee")));

		InvocationTargetException e = new InvocationTargetException(new ClassNotFoundException("abc"));
		assertEquals("java.lang.ClassNotFoundException: abc", SaturnUtils.getErrorMessage(e));

		assertEquals("", SaturnUtils.getErrorMessage(null));
	}

}