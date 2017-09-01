package com.vip.saturn.it.utils;

import com.google.common.base.Predicate;

/**
 * Models a condition that might reasonably be expected to eventually evaluate to something that is neither null nor
 * false. Examples would include determining if a job execution completed zk node has appeared or process count changed.
 * Thread.sleep() could work in most cases, but usually if you're waiting, you are actually waiting for a particular
 * condition or state to occur. Thread.sleep() does not guarantee that whatever you're waiting for has actually
 * happened. For more reliable and more expected behaviour, wait for this expected condition
 *
 * Created by gilbert.guo on 2016/9/26.
 */
public interface ExpectedCondition extends Predicate {
}
