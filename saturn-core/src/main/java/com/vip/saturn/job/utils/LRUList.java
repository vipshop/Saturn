/**
 *
 */
package com.vip.saturn.job.utils;

import java.util.LinkedList;

/**
 * @author chembo.huang
 */
public final class LRUList<E> extends LinkedList<E> {

	private static final long serialVersionUID = 1L;

	private final int mMaxSize;

	public LRUList(int pMaxSize) {
		mMaxSize = pMaxSize;
	}

	public void put(E e) {
		if (this.size() >= mMaxSize) {
			this.removeFirst();
		}
		this.addLast(e);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}

		LRUList<?> lruList = (LRUList<?>) o;

		return mMaxSize == lruList.mMaxSize;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + mMaxSize;
		return result;
	}
}
