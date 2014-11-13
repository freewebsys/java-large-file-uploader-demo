package com.am.jlfu.fileuploader.utils;

import java.io.FileNotFoundException;
import java.util.Set;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.unitils.mock.Mock;
import org.unitils.mock.core.MockObject;

import com.am.jlfu.fileuploader.utils.ProgressManager.ProgressManagerAdvertiser;
import com.am.jlfu.notifier.JLFUListenerPropagator;
import com.am.jlfu.staticstate.entities.FileProgressStatus;
import com.google.common.collect.Sets;

@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class ProgressManagerTest {

	@Autowired
	ClientToFilesMap clientToFilesMap;
	
	@Autowired
	ProgressManager progressManager;
	
	@Autowired
	JLFUListenerPropagator jlfuListenerPropagator;
	
	Mock<ProgressCalculator> progressCalculator = new MockObject<ProgressCalculator>(ProgressCalculator.class, new Object());
	Mock<ProgressManagerAdvertiser> progressManagerAdvertiser = new MockObject<ProgressManagerAdvertiser>(ProgressManagerAdvertiser.class, new Object());
	
	private UUID clientId = UUID.randomUUID();
	private UUID fileId = UUID.randomUUID();
	
	@Before
	public void init() {
		
		//init client to files map
		clientToFilesMap.clear();
		Set<UUID> newHashSet = Sets.newHashSet();
		clientToFilesMap.put(clientId, newHashSet);
		newHashSet.add(fileId);
		
		//reset progress manager map
		progressManager.fileToProgressInfo.clear();
		
		//set mock
		ReflectionTestUtils.setField(progressManager, "progressCalculator", progressCalculator.getMock());
		ReflectionTestUtils.setField(progressManager, "progressManagerAdvertiser", progressManagerAdvertiser.getMock());
		
	}
	
	@Test
	public void testWithProgress() throws FileNotFoundException {
		assertReturnedIsCorrect(15f, true);
		assertReturnedIsCorrect(30f, true);
		assertReturnedIsCorrect(30f, false);
	}

	private void assertReturnedIsCorrect(float returnedValue, boolean shallBePropagated)
			throws FileNotFoundException {
		
		//mock service
		FileProgressStatus fileProgressStatus = new FileProgressStatus();
		fileProgressStatus.setProgress(returnedValue);
		progressCalculator.onceReturns(fileProgressStatus).getProgress(clientId, fileId);
		
		//calculate progress
		progressManager.calculateProgress();
		
		//assert map is filled
		Assert.assertThat(progressManager.fileToProgressInfo.get(fileId).getProgress(), CoreMatchers.is(returnedValue));
		
		//assert that event is propagated
		if (shallBePropagated) {
			progressManagerAdvertiser.assertInvoked().advertise(clientId, fileId, fileProgressStatus);
		} else {
			progressManagerAdvertiser.assertNotInvoked().advertise(clientId, fileId, fileProgressStatus);
		}
	}
	
	
}
