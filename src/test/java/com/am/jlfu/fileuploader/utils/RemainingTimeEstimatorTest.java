package com.am.jlfu.fileuploader.utils;

import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.staticstate.entities.FileProgressStatus;

@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class RemainingTimeEstimatorTest {

	@Autowired
	RemainingTimeEstimator remainingTimeEstimator;

	@Test
	public void testGetRemainingTime() {
		UUID clientId = UUID.randomUUID();
		
		FileProgressStatus progress = new FileProgressStatus();
		progress.setTotalFileSize(1000);
		progress.setBytesUploaded(0);
		Assert.assertThat(remainingTimeEstimator.getRemainingTime(clientId, progress, 100l), CoreMatchers.is(10l));
		Assert.assertThat(remainingTimeEstimator.getRemainingTime(clientId, progress, 300l), CoreMatchers.is(5l));
		Assert.assertThat(remainingTimeEstimator.getRemainingTime(clientId, progress, 200l), CoreMatchers.is(5l));
		Assert.assertThat(remainingTimeEstimator.getRemainingTime(clientId, progress, 200l), CoreMatchers.is(5l));
		Assert.assertThat(remainingTimeEstimator.getRemainingTime(clientId, progress, 200l), CoreMatchers.is(5l));
		Assert.assertThat(remainingTimeEstimator.getRemainingTime(clientId, progress, 200l), CoreMatchers.is(5l));
		Assert.assertThat(remainingTimeEstimator.getRemainingTime(clientId, progress, 200l), CoreMatchers.is(5l));
		Assert.assertThat(remainingTimeEstimator.getRemainingTime(clientId, progress, 200l), CoreMatchers.is(5l));
		Assert.assertThat(remainingTimeEstimator.getRemainingTime(clientId, progress, 200l), CoreMatchers.is(5l));
		Assert.assertThat(remainingTimeEstimator.getRemainingTime(clientId, progress, 200l), CoreMatchers.is(5l));
		Assert.assertThat(remainingTimeEstimator.getRemainingTime(clientId, progress, 200l), CoreMatchers.not(5l));
	}
	
	
	
	@Test
	public void testCalculateRemainingTime() {
		processRemainingTimeTest(1000l, 0l, 100l, 10l);
		processRemainingTimeTest(1000l, 500l, 100l, 5l);
		processRemainingTimeTest(1000l, 1000l, 100l, 1l);
	}


	private void processRemainingTimeTest(long fileSize, long start, long rate, long expectedSeconds) {
		FileProgressStatus progress = new FileProgressStatus();
		progress.setTotalFileSize(fileSize);
		progress.setBytesUploaded(start);
		long calculateRemainingTime = remainingTimeEstimator.calculateRemainingTime(progress, rate);
		Assert.assertThat(calculateRemainingTime, CoreMatchers.is(expectedSeconds));
	}
	
	
	
}
