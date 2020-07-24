/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

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
