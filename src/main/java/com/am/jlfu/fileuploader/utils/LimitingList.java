package com.am.jlfu.fileuploader.utils;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * List that limits to a certain number of items. All items at the end of the list will be removed. 
 * @author antoinem
 *
 * @param <T>
 */
public class LimitingList<T> {

	List<T> list = Lists.newArrayList();

	private int limit;
	
	public LimitingList(int limit) {
		super();
		this.limit = limit;
	}
	
	/**
	 * Adds an element at the beginning of the list. 
	 * @param element
	 */
	public void unshift(T element) {

		//unshift
		list.add(0,element);
		
		//process removal
		if(list.size() > limit) {
			list.remove(limit);
		}
	}
	
	
	public List<T> getList() {
		return list;
	}
	
}
