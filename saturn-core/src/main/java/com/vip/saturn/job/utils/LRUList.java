/**
 * 
 */
package com.vip.saturn.job.utils;

import java.util.LinkedList;

/**
 * @author chembo.huang
 *
 */
public final class LRUList<E> extends LinkedList<E> {
	
	private static final long serialVersionUID = 1L;
	
	private final int  mMaxSize;
	
	public LRUList(int pMaxSize) {mMaxSize = pMaxSize;}
	
	public final void put(E e){
		if( this.size() >= mMaxSize ){
			this.removeFirst();
		}
		this.addLast(e);
	}
}
