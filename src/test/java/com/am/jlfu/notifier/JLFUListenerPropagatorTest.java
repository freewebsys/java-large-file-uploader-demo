package com.am.jlfu.notifier;


import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.staticstate.entities.FileProgressStatus;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class JLFUListenerPropagatorTest {

	@Autowired
	JLFUListenerPropagator jlfuListenerPropagator;

	private volatile int testCounter;

	private JLFUListenerAdapter listener;



	@Before
	public void before() {
		jlfuListenerPropagator.unregisterAllListeners();
		
		testCounter = 0;
		listener = new JLFUListenerAdapter() {

			@Override
			public void onNewClient(UUID clientId) {
				testCounter++;
			}
		};

	}


	@Test
	public void test() throws InterruptedException {

		// add two listener
		jlfuListenerPropagator.registerListener(listener);
		jlfuListenerPropagator.registerListener(listener);

		// trigger event
		jlfuListenerPropagator.getPropagator().onNewClient(UUID.randomUUID());
		Thread.sleep(100);

		// assert
		Assert.assertThat(testCounter, CoreMatchers.is(2));

		// unregister one listener
		jlfuListenerPropagator.unregisterListener(listener);

		// trigger event
		jlfuListenerPropagator.getPropagator().onNewClient(UUID.randomUUID());
		Thread.sleep(100);

		// assert
		Assert.assertThat(testCounter, CoreMatchers.is(3));

	}
	
	@Test
	public void testNotBlocked() {
		jlfuListenerPropagator.registerListener(new JLFUListenerAdapter() {
			@Override
			public void onClientBack(UUID clientId) {
				try {
					Thread.sleep(10000);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				Assert.fail();
			}
		});
		jlfuListenerPropagator.getPropagator().onClientBack(UUID.randomUUID());
	}
	
	@Test
	public void log() {
		jlfuListenerPropagator.registerListener(new JLFUListenerAdapter());
		FileProgressStatus progress = new FileProgressStatus();
		progress.setBytesUploaded(123);
		progress.setProgress(1234f);
		progress.setTotalFileSize(12340124);
		jlfuListenerPropagator.getPropagator().onFileUploadProgress(UUID.randomUUID(),UUID.randomUUID(), progress);
	}
	
}
