package com.am.jlfu.fileuploader.limiter;


import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.fileuploader.logic.UploadServletAsyncProcessor;
import com.google.common.collect.Lists;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
// TODO make tests to check master and client limitations
public class RateLimiterTest {

	private static final Logger log = LoggerFactory.getLogger(RateLimiterTest.class);

	@Autowired
	RateLimiterConfigurationManager uploadProcessingConfigurationManager;

	@Autowired
	UploadProcessingOperationManager uploadProcessingOperationManager;


	ExecutorService executorService = Executors.newFixedThreadPool(10);



	private void emulateUpload(Long requestRate, Long clientRate, Long masterRate, int uploadSizeInKB, int expectedDuration)
			throws InterruptedException {
		UUID fileId = UUID.randomUUID();
		UUID clientId = UUID.randomUUID();

		// extract config
		final RequestUploadProcessingConfiguration uploadProcessingConfiguration =
				uploadProcessingConfigurationManager.getUploadProcessingConfiguration(fileId);
		final UploadProcessingConfiguration clientProcessingConfiguration =
				uploadProcessingConfigurationManager.getUploadProcessingConfiguration(clientId);
		final UploadProcessingConfiguration masterProcessingConfiguration =
				uploadProcessingConfigurationManager.getMasterProcessingConfiguration();

		// extract operation
		uploadProcessingOperationManager.startOperation(clientId, fileId);
		final UploadProcessingOperation clientProcessingOperation = uploadProcessingOperationManager.getClientProcessingOperation(clientId);
		final UploadProcessingOperation fileProcessingOperation = uploadProcessingOperationManager.getFileProcessingOperation(fileId);
		final UploadProcessingOperation masterProcessingOperation = uploadProcessingOperationManager.getMasterProcessingOperation();

		// init request rate
		if (requestRate != null) {
			assignRateToRequest(requestRate, fileId, uploadProcessingConfiguration, fileProcessingOperation);
		}

		// perform upload
		long itTookThatLong =
				upload(clientId, fileId, uploadSizeInKB, uploadProcessingConfiguration, clientProcessingConfiguration, masterProcessingConfiguration,
						fileProcessingOperation, clientProcessingOperation, masterProcessingOperation);

		// specify completion
		uploadProcessingConfigurationManager.reset(fileId);

		// verify that it took around that duration
		itTookAround(expectedDuration, itTookThatLong);

	}


	private void itTookAround(int expectedDuration, Long itTookThatLong) {
		Assert.assertThat(itTookThatLong.floatValue(), lessThan((float) expectedDuration * 1.2f));
		Assert.assertThat(itTookThatLong.floatValue(), greaterThan((float) expectedDuration * 0.8f));
	}


	private void assignRateToRequest(Long requestRate, UUID id, final RequestUploadProcessingConfiguration uploadProcessingConfiguration,
			UploadProcessingOperation fileProcessingOperation) {

		uploadProcessingConfiguration.setProcessing(true);
		final long originalDownloadAllowanceForIteration = fileProcessingOperation.getDownloadAllowanceForIteration();
		uploadProcessingConfigurationManager.assignRateToRequest(id, requestRate);

		// wait for the rate modification to occur
		while (fileProcessingOperation.getDownloadAllowanceForIteration() == originalDownloadAllowanceForIteration) {
		}
	}


	private long upload(UUID clientId, UUID fileId, int uploadSizeInKB,
			RequestUploadProcessingConfiguration requestUploadProcessingConfiguration,
			UploadProcessingConfiguration clientUploadProcessingConfiguration, UploadProcessingConfiguration masterUploadProcessingConfiguration,
			UploadProcessingOperation fileProcessingOperation, UploadProcessingOperation clientProcessingOperation,
			UploadProcessingOperation masterProcessingOperation) {

		// set the request as processing
		requestUploadProcessingConfiguration.setProcessing(true);

		// emulate an upload of a file
		long totalUpload = uploadSizeInKB * 1024;
		final Date reference = new Date();
		long allowance;
		while (totalUpload > 0) {

			// calculate allowance
			allowance = UploadServletAsyncProcessor.minOf(
					(int) fileProcessingOperation.getDownloadAllowanceForIteration(),
					(int) clientProcessingOperation.getDownloadAllowanceForIteration(),
					(int) masterProcessingOperation.getDownloadAllowanceForIteration()
					);

			// consumption
			fileProcessingOperation.bytesConsumedFromAllowance(allowance);
			clientProcessingOperation.bytesConsumedFromAllowance(allowance);
			masterProcessingOperation.bytesConsumedFromAllowance(allowance);

			totalUpload -= allowance;
			log.debug(clientId + " " + fileId + " uploaded " + totalUpload);
		}
		return new Date().getTime() - reference.getTime();


	}


	@Test
	public void testMonoRequestLimitation()
			throws ExecutionException, InterruptedException {

		// lets say we upload a 1MB file.
		int upload = 1000;

		// set rate to 1MB, should have taken 1 second
		log.debug("testMonoRequestLimitation 1MB");
		emulateUpload(1000l, null, null, upload, 1000);

		// set rate to 0.5MB, should have taken 2second
		log.debug("testMonoRequestLimitation 2MB");
		emulateUpload(500l, null, null, upload, 2000);

	}


	@Test
	public void testClientRateLimitation()
			throws InterruptedException, ExecutionException {
		// client will limit
		testClientMaster(10000, 100000);

		// master will limit
		testClientMaster(100000, 10000);
	}


	private void testClientMaster(int client, int master)
			throws InterruptedException {

		// set client rate limitation
		uploadProcessingConfigurationManager.setMaximumRatePerClientInKiloBytes(client);

		// set master rate limitation
		uploadProcessingConfigurationManager.setMaximumOverAllRateInKiloBytes(master);

		final UUID clientId = UUID.randomUUID();

		// and 10 requests are gonna upload a 10MB file
		int numberOfRequests = 10;
		List<Callable<Long>> runnables = Lists.newArrayList();
		for (int i = 0; i < numberOfRequests; i++) {
			runnables.add(new TestRunnable(clientId, UUID.randomUUID(), 10000));
		}

		// invoke
		final Date reference = new Date();
		executorService.invokeAll(runnables);

		// shall have taken around 10seconds for all of them to complete
		itTookAround(10000, new Date().getTime() - reference.getTime());
	}



	class TestRunnable
			implements Callable<Long> {

		private UUID clientId;
		private UUID fileId;
		private int fileSize;
		private RequestUploadProcessingConfiguration requestUploadProcessingConfiguration;
		private UploadProcessingConfiguration clientUploadProcessingConfiguration;
		private UploadProcessingConfiguration masterUploadProcessingConfiguration;
		private UploadProcessingOperation fileProcessingOperation;
		private UploadProcessingOperation masterProcessingOperation;
		private UploadProcessingOperation clientProcessingOperation;



		public TestRunnable(UUID clientId, UUID fileId, int fileSize) {
			this.clientId = clientId;
			this.fileId = fileId;
			this.fileSize = fileSize;
			requestUploadProcessingConfiguration =
					uploadProcessingConfigurationManager.getUploadProcessingConfiguration(fileId);
			clientUploadProcessingConfiguration =
					uploadProcessingConfigurationManager.getUploadProcessingConfiguration(clientId);
			masterUploadProcessingConfiguration =
					uploadProcessingConfigurationManager.getMasterProcessingConfiguration();
			uploadProcessingOperationManager.startOperation(clientId, fileId);
			fileProcessingOperation = uploadProcessingOperationManager.getFileProcessingOperation(clientId);
			clientProcessingOperation = uploadProcessingOperationManager.getClientProcessingOperation(fileId);
			masterProcessingOperation = uploadProcessingOperationManager.getMasterProcessingOperation();
		}


		@Override
		public Long call() {
			return upload(clientId, fileId, fileSize, requestUploadProcessingConfiguration, clientUploadProcessingConfiguration,
					masterUploadProcessingConfiguration, fileProcessingOperation, clientProcessingOperation, masterProcessingOperation);
		}
	}


}
