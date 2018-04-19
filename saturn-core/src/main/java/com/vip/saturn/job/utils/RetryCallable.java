package com.vip.saturn.job.utils;

public interface RetryCallable<V> {

	V call() throws Exception;
}
