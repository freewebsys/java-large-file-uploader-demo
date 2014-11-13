package com.am.jlfu.fileuploader.logic;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.lessThan;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.fileuploader.exception.FileCorruptedException;
import com.am.jlfu.fileuploader.exception.InvalidCrcException;
import com.am.jlfu.fileuploader.exception.MissingParameterException;
import com.am.jlfu.fileuploader.json.CRCResult;
import com.am.jlfu.fileuploader.limiter.RateLimiterConfigurationManager;
import com.am.jlfu.fileuploader.logic.UploadProcessorTest.TestFileSplitResult;
import com.am.jlfu.fileuploader.logic.UploadServletAsyncProcessor.WriteChunkCompletionListener;
import com.am.jlfu.fileuploader.utils.CRCHelper;
import com.am.jlfu.fileuploader.utils.ProgressCalculator;
import com.am.jlfu.fileuploader.web.utils.RequestComponentContainer;
import com.am.jlfu.staticstate.StaticStateIdentifierManager;
import com.am.jlfu.staticstate.StaticStateManager;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class UploadServletAsyncProcessorTest {

	private static final Logger log = LoggerFactory.getLogger(UploadServletAsyncProcessorTest.class);
	public static final int WAIT_THAT_TIME_FOR_LOCKS_IN_MILLISECONDS = 2000;

	@Autowired
	RateLimiterConfigurationManager rateLimiterConfigurationManager;

	@Autowired
	CRCHelper crcHelper;

	@Autowired
	UploadServletAsyncProcessor uploadServletAsyncProcessor;

	@Autowired
	UploadProcessor uploadProcessor;

	@Autowired
	RequestComponentContainer requestComponentContainer;

	@Autowired
	StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;

	@Autowired
	ProgressCalculator progressCalculator;

	@Autowired
	StaticStateIdentifierManager staticStateIdentifierManager;

	MockMultipartFile tinyFile;
	Long tinyFileSize;
	byte[] tinyFileContent;

	String fileName = "zenameofzefile.owf";



	@Before
	public void init()
			throws IOException, InterruptedException, ExecutionException, TimeoutException {

		// populate request component container
		requestComponentContainer.populate(new MockHttpServletRequest(), new MockHttpServletResponse());

		// clear state
		staticStateManager.clear();

		// init file
		tinyFileContent = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
		tinyFile = new MockMultipartFile("blob", tinyFileContent);
		tinyFileSize = Integer.valueOf(tinyFileContent.length).longValue();
	}



	private class Listener
			implements WriteChunkCompletionListener {

		private boolean releaseOnSuccess;
		private Exception e;
		private UUID clientId;
		private UUID fileId;



		public Listener(UUID clientId, UUID fileId, boolean shallSucceed) {
			this.releaseOnSuccess = shallSucceed;
			this.clientId = clientId;
			this.fileId = fileId;
		}


		@Override
		public void error(Exception exception) {
			uploadServletAsyncProcessor.clean(clientId, fileId);
			e = exception;
			if (releaseOnSuccess) {
				Assert.fail();
			}
			release();
		}


		@Override
		public void success() {
			uploadServletAsyncProcessor.clean(clientId, fileId);
			if (!releaseOnSuccess) {
				Assert.fail();
			}
			release();
		}


		void release() {
			synchronized (Listener.this) {
				Listener.this.notifyAll();
			}
		}

	}



	@Test
	public void testInvalidCrc()
			throws IOException, MissingParameterException, FileUploadException, InvalidCrcException, InterruptedException {

		// begin a file upload process
		UUID fileId = uploadProcessor.prepareUpload(tinyFileSize, fileName, "lala");

		// upload with bad crc
		TestFileSplitResult splitResult = UploadProcessorTest.getByteArrayFromFile(tinyFile, 0, 3);
		splitResult.crc = "lala";

		processWaitForCompletionAndCheck(fileId, splitResult, InvalidCrcException.class);
	}


	private void waitForListener(Listener completionListener)
			throws InterruptedException {
		synchronized (completionListener) {
			completionListener.wait(WAIT_THAT_TIME_FOR_LOCKS_IN_MILLISECONDS);
		}
	}


	@Test
	public void testClassicGranular()
			throws ServletException, IOException, InvalidCrcException, MissingParameterException, FileUploadException,
			InterruptedException {
		TestFileSplitResult splitResult;

		// begin a file upload process
		UUID fileId = uploadProcessor.prepareUpload(tinyFileSize, fileName, "lala");
		CRCResult bufferedCrc = crcHelper.getBufferedCrc(new ByteArrayInputStream(tinyFileContent.clone()));

		// get progress
		Assert.assertThat(0f, is(uploadProcessor.getProgress(fileId).getProgress()));

		// upload first part
		splitResult = UploadProcessorTest.getByteArrayFromFile(tinyFile, 0, 3);
		processWaitForCompletionAndCheck(fileId, splitResult);

		// get progress
		Assert.assertThat(Math.round(progressCalculator.getProgress(staticStateIdentifierManager.getIdentifier(), fileId).getProgress()), is(3 * 100 / tinyFileSize.intValue()));

		// upload second part
		splitResult = UploadProcessorTest.getByteArrayFromFile(tinyFile, 3, 5);
		processWaitForCompletionAndCheck(fileId, splitResult);

		// get progress
		Assert.assertThat(Math.round(progressCalculator.getProgress(staticStateIdentifierManager.getIdentifier(), fileId).getProgress()), is(Math.round(5f / tinyFileSize.floatValue() * 100f)));

		// upload last part
		splitResult = UploadProcessorTest.getByteArrayFromFile(tinyFile, 5, tinyFileSize.intValue());
		processWaitForCompletionAndCheck(fileId, splitResult);

		// get progress
		Assert.assertThat(Math.round(progressCalculator.getProgress(staticStateIdentifierManager.getIdentifier(), fileId).getProgress()), is(100));

		// check crc
		CRCResult fileCrc =
				crcHelper.getBufferedCrc(new FileInputStream(new File(staticStateManager.getEntity().getFileStates().get(fileId)
						.getAbsoluteFullPathOfUploadedFile())));
		Assert.assertThat(fileCrc, is(bufferedCrc));
	}



	private class RunnableInTheProcessWithStreamDisconnection extends RunnableInTheProcess {

		/**
		 * 1 for first<br>
		 * 2 for middle<br>
		 * 3 for last<br>
		 */
		private int sliceToFailAtCode;
		private boolean invalidCrc;



		public RunnableInTheProcessWithStreamDisconnection(int sliceToFailAtCode, boolean invalidCrc) {
			this.sliceToFailAtCode = sliceToFailAtCode;
			this.invalidCrc = invalidCrc;
		}


		@Override
		protected void run()
				throws Exception {

			// prepare that slice
			String absoluteFullPathOfUploadedFile =
					staticStateManager.getEntity().getFileStates().get(fileId).getAbsoluteFullPathOfUploadedFile();
			File file = new File(absoluteFullPathOfUploadedFile);
			long destination = uploadProcessor.getSliceSizeInBytes() * currentSlice + uploadProcessor.getSliceSizeInBytes();
			TestFileSplitResult byteArrayFromFile =
					UploadProcessorTest.getByteArrayFromInputStream(new ByteArrayInputStream(fileContent), uploadProcessor.getSliceSizeInBytes() *
							currentSlice, destination);

			int sliceToFailAt = -1;
			switch (sliceToFailAtCode) {
				case 0:
					sliceToFailAt = 0;
					break;
				case 1:
					sliceToFailAt = (int) (numberOfSlices / 2);
					break;
				case 2:
					sliceToFailAt = (int) numberOfSlices;
					break;
			}

			// if this is slice that sould fail
			if (currentSlice == sliceToFailAt) {

				// provides a stream that will fail fast
				try {
					byteArrayFromFile.stream =
							new ByteArrayInputStreamThatFails(uploadProcessor.getSliceSizeInBytes(), IOUtils.toByteArray(byteArrayFromFile.stream));
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}

				// and process
				processWaitForCompletionAndCheck(fileId, byteArrayFromFile, Exception.class);

				// assert that the validated crc is of the size of the slices that were successfull
				Long crcedBytesBeforeVerification =
						staticStateManager.getEntity().getFileStates().get(fileId).getStaticFileStateJson().getCrcedBytes();
				Assert.assertThat(crcedBytesBeforeVerification, is(sliceToFailAt * uploadProcessor.getSliceSizeInBytes()));

				// assert that we have written the correct amount
				long size = file.length();
				long sliceMissingSize =
						((ByteArrayInputStreamThatFails) byteArrayFromFile.stream).failAt * UploadServletAsyncProcessor.SIZE_OF_THE_BUFFER_IN_BYTES;
				long completedPart = sliceMissingSize +
						(currentSlice * uploadProcessor.getSliceSizeInBytes());
				Assert.assertThat(size, is(completedPart));

				// process the crc of the part that has not been completed
				byteArrayFromFile =
						UploadProcessorTest.getByteArrayFromInputStream(new FileInputStream(file), crcedBytesBeforeVerification, completedPart);

				// change the crc with a fake one if invalidity check
				if (invalidCrc) {
					byteArrayFromFile.crc = "invalid";
				}

				Long newCrcedBytes;
				// process the crc validation of the previous chunk
				try {
					uploadProcessor.verifyCrcOfUncheckedPart(fileId, byteArrayFromFile.crc);
					// we should have an exception if we are using an invalid crc
					if (invalidCrc) {
						Assert.fail();
					}

					// assert that the validated amount is now more than the previous one
					newCrcedBytes = staticStateManager.getEntity().getFileStates().get(fileId).getStaticFileStateJson().getCrcedBytes();
					Assert.assertThat(crcedBytesBeforeVerification, lessThan(newCrcedBytes));

				}
				catch (InvalidCrcException ee) {

					// we are invalid, the crc size shall be unchanged
					newCrcedBytes = staticStateManager.getEntity().getFileStates().get(fileId).getStaticFileStateJson().getCrcedBytes();
					Assert.assertThat(newCrcedBytes, is(crcedBytesBeforeVerification));

					// re-process the slice from beginning

				}

				// assert that the file is still matching the validated, either truncated or
				// appended.
				size = file.length();
				Assert.assertThat(newCrcedBytes, is(size));

				// finish this slice
				byteArrayFromFile =
						UploadProcessorTest.getByteArrayFromInputStream(new ByteArrayInputStream(fileContent), newCrcedBytes, destination);


				// process it
				processWaitForCompletionAndCheck(fileId, byteArrayFromFile);

			}
			// otherwise process normally
			else {

				// process it
				processWaitForCompletionAndCheck(fileId, byteArrayFromFile);
			}

		}
	}



	@Test
	public void testStreamDisconnectionInFirstSlice()
			throws Exception {
		testFileComplete(new RunnableInTheProcessWithStreamDisconnection(0, false));
	}


	@Test
	public void testStreamDisconnectionInFirstSliceWithInvalidity()
			throws Exception {
		testFileComplete(new RunnableInTheProcessWithStreamDisconnection(0, true));
	}


	@Test
	public void testStreamDisconnectionInMiddleSlice()
			throws Exception {
		testFileComplete(new RunnableInTheProcessWithStreamDisconnection(1, false));
	}


	@Test
	public void testStreamDisconnectionInMiddleSliceWithInvalidity()
			throws Exception {
		testFileComplete(new RunnableInTheProcessWithStreamDisconnection(1, true));
	}


	@Test
	public void testStreamDisconnectionInLastSlice()
			throws Exception {
		testFileComplete(new RunnableInTheProcessWithStreamDisconnection(2, false));
	}


	@Test
	public void testStreamDisconnectionInLastSliceWithInvalidity()
			throws Exception {
		testFileComplete(new RunnableInTheProcessWithStreamDisconnection(2, true));
	}


	@Test
	public void testBigFileComplete()
			throws Exception {
		testFileComplete(new RunnableInTheProcess() {

			@Override
			protected void run()
					throws Exception {

				// prepare that slice
				staticStateManager.getEntity().getFileStates().get(fileId).getAbsoluteFullPathOfUploadedFile();
				TestFileSplitResult byteArrayFromFile =
						UploadProcessorTest.getByteArrayFromInputStream(new ByteArrayInputStream(fileContent), uploadProcessor.getSliceSizeInBytes() *
								currentSlice, uploadProcessor.getSliceSizeInBytes() * currentSlice +
								uploadProcessor.getSliceSizeInBytes());

				// process it
				processWaitForCompletionAndCheck(fileId, byteArrayFromFile);


			}
		});
	}


	@Test
	public void testBigFileWithPauseAndResume()
			throws Exception {
		testFileComplete(new RunnableInTheProcess() {

			@Override
			public void run()
					throws InterruptedException, IOException {

				// prepare that slice
				String absoluteFullPathOfUploadedFile =
						staticStateManager.getEntity().getFileStates().get(fileId).getAbsoluteFullPathOfUploadedFile();
				TestFileSplitResult byteArrayFromFile =
						UploadProcessorTest.getByteArrayFromInputStream(new ByteArrayInputStream(fileContent), uploadProcessor.getSliceSizeInBytes() *
								currentSlice, uploadProcessor.getSliceSizeInBytes() * currentSlice +
								uploadProcessor.getSliceSizeInBytes());


				// at one point, pause it:
				if (currentSlice == numberOfSlices / 2) {

					// pause
					uploadProcessor.pauseFile(Arrays.asList(new UUID[] {fileId}));

					// get the file size
					long length = new File(absoluteFullPathOfUploadedFile).length();

					// wait a bit
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException e) {
						throw new RuntimeException(e);
					}

					//process it, it should not be processed as the file is paused
					processWaitForCompletionAndCheck(fileId, byteArrayFromFile);

					// assert the size is the same
					Assert.assertThat(new File(absoluteFullPathOfUploadedFile).length(), is(length));

					// then continue processing
					uploadProcessor.resumeFile(fileId);

				}

				//process
				processWaitForCompletionAndCheck(fileId, byteArrayFromFile);

			}
		});
	}



	private abstract class RunnableInTheProcess
	{

		protected int currentSlice;
		protected long numberOfSlices;
		protected UUID fileId;
		protected byte[] fileContent;



		protected abstract void run()
				throws Exception;


		public void start(Semaphore referenceToWakeUp, int currentSlice, long numberOfSlices, UUID fileId, byte[] fileContent)
				throws Exception {
			this.currentSlice = currentSlice;
			this.numberOfSlices = numberOfSlices;
			this.fileId = fileId;
			this.fileContent = fileContent;
			try {
				run();
			}
			finally {
				referenceToWakeUp.release();
			}
		}
	}



	public void testFileComplete(RunnableInTheProcess doSomethingInTheMiddle)
			throws Exception {
		Semaphore waitForMe = new Semaphore(0);

		// init a file which is about 115 MB (we want to check out-of-buffer
		// granularity, so not an
		// exact value)
		long size = 121123456;
		byte[] fileContent = new byte[(int) size];
		new Random().nextBytes(fileContent);

		// prepare upload
		UUID fileId = uploadProcessor.prepareUpload(size, fileName, "lala");
		String absoluteFullPathOfUploadedFile = staticStateManager.getEntity().getFileStates().get(fileId).getAbsoluteFullPathOfUploadedFile();

		// set a 100mb rate
		rateLimiterConfigurationManager.setMaximumRatePerClientInKiloBytes(100 * 1024);
		rateLimiterConfigurationManager.setMaximumOverAllRateInKiloBytes(100 * 1024);

		// for all the slices that we need to send
		long numberOfSlices = size / uploadProcessor.getSliceSizeInBytes();
		for (int currentSlice = 0; currentSlice < numberOfSlices + 1; currentSlice++) {

			// perform treatment
			if (doSomethingInTheMiddle != null) {
				doSomethingInTheMiddle.start(waitForMe, currentSlice, numberOfSlices, fileId, fileContent);
				Assert.assertTrue(waitForMe.tryAcquire(WAIT_THAT_TIME_FOR_LOCKS_IN_MILLISECONDS, TimeUnit.MINUTES));
			}

		}

		// now calculates the crc of sent file
		String valueSource = crcHelper.getBufferedCrc(new ByteArrayInputStream(fileContent)).getCrcAsString();

		// and the one of received file
		String valueCopied = crcHelper.getBufferedCrc(new FileInputStream(new File(absoluteFullPathOfUploadedFile))).getCrcAsString();

		// assert the same
		Assert.assertThat(valueCopied, is(valueSource));

	}


	private void processWaitForCompletionAndCheck(UUID fileId, TestFileSplitResult byteArrayFromFile)
			throws FileNotFoundException, InterruptedException {
		processWaitForCompletionAndCheck(fileId, byteArrayFromFile, null);
	}


	private void processWaitForCompletionAndCheck(UUID fileId, TestFileSplitResult byteArrayFromFile, Class<? extends Exception> expectedException)
			throws FileNotFoundException, InterruptedException {
		Listener completionListener = new Listener(staticStateIdentifierManager.getIdentifier(), fileId, expectedException == null);
		uploadServletAsyncProcessor.process(staticStateManager.getEntity().getFileStates().get(fileId), fileId, byteArrayFromFile.crc,
				byteArrayFromFile.stream, completionListener);
		waitForListener(completionListener);
		if (expectedException == null) {
			Assert.assertThat(completionListener.e, nullValue());
		}
		else {
			Assert.assertTrue(expectedException.isInstance(completionListener.e));
		}
	}



	private class ByteArrayInputStreamThatFails extends ByteArrayInputStream {

		// fail in the middle of a slice
		long failAt;
		int i;



		public ByteArrayInputStreamThatFails(long sliceSizeInBytes, byte[] buf) {
			super(buf);
			failAt = sliceSizeInBytes / UploadServletAsyncProcessor.SIZE_OF_THE_BUFFER_IN_BYTES / 2;
		}


		@Override
		public int read(byte[] b)
				throws IOException {
			if (i++ == failAt) {
				throw new IOException("Stream ended unexpectedly");
			}
			return super.read(b);
		}
	}
	

	@Test(expected = FileCorruptedException.class)
	public void testFileCorruptedException() throws IOException, InterruptedException, InvalidCrcException, FileCorruptedException {

		// begin a file upload process
		UUID fileId = uploadProcessor.prepareUpload(tinyFileSize, fileName, "lala");
		staticStateManager.setCrcBytesValidated(staticStateIdentifierManager.getIdentifier(), fileId, 10);
		
	}

}
