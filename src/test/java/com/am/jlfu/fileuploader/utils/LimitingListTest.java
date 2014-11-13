package com.am.jlfu.fileuploader.utils;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class LimitingListTest {

	
	@Test
	public void test() {
		Integer a = 1;
		Integer b = 2;
		Integer c = 3;
		Integer d = 4;
		LimitingList<Integer> limitingList = new LimitingList<Integer>(2);
		limitingList.unshift(a);
		limitingList.unshift(b);
		Assert.assertThat(limitingList.list.get(0), CoreMatchers.is(b));
		Assert.assertThat(limitingList.list.get(1), CoreMatchers.is(a));
		limitingList.unshift(c);
		Assert.assertThat(limitingList.list.get(0), CoreMatchers.is(c));
		Assert.assertThat(limitingList.list.get(1), CoreMatchers.is(b));
		Assert.assertThat(limitingList.list.size(), CoreMatchers.is(2));
		limitingList.unshift(d);
		Assert.assertThat(limitingList.list.get(0), CoreMatchers.is(d));
		Assert.assertThat(limitingList.list.get(1), CoreMatchers.is(c));
		Assert.assertThat(limitingList.list.size(), CoreMatchers.is(2));
		
		
	}
	
	
}
