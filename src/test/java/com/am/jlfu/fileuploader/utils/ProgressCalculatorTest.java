package com.am.jlfu.fileuploader.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class ProgressCalculatorTest {

	@Autowired
	ProgressCalculator progressCalculator;
	
	@Test
	public void progressCalculationTest() {
		// check basic 30% (30/100)
		Assert.assertThat(Double.valueOf(30), is(progressCalculator.calculateProgress(30l, 100l)));
		Long bigValue = 1000000000000000000l;
		// check that we dont return 100% if values are not exactly equals
		Assert.assertThat(Double.valueOf(100), is(not(progressCalculator.calculateProgress(bigValue - 1, bigValue))));
		// check that we return 100% if values are equals
		Assert.assertThat(Double.valueOf(100), is(progressCalculator.calculateProgress(bigValue, bigValue)));
		// check that we return 0% when 0/x
		Assert.assertThat(Double.valueOf(0), is(progressCalculator.calculateProgress(0l, 240l)));
	}
}
