package com.am.jlfu.fileuploader.limiter;


import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;



@Component
public class RateLimiter
{


	private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

	@Autowired
	RateLimiterConfigurationManager uploadProcessingConfigurationManager;

	@Autowired
	UploadProcessingOperationManager uploadProcessingOperationManager;

	/** Number of times the bucket is filled per second. */
	public static final int NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND = 10;
	public static final long BUCKET_FILLED_EVERY_X_MILLISECONDS = DateUtils.MILLIS_PER_SECOND / NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND;



	@Scheduled(fixedRate = BUCKET_FILLED_EVERY_X_MILLISECONDS)
	public void fillBucket() {

		// first we need to calculate how many uploads are currently being processed
		int requestsBeingProcessed = 0;
		for (Entry<UUID, RequestUploadProcessingConfiguration> entry : uploadProcessingConfigurationManager.getRequestEntries()) {
			requestsBeingProcessed += entry.getValue().isProcessing() ? 1 : 0;
		}
		log.trace("refilling the upload allowance of the " + requestsBeingProcessed + " uploads being processed");

		// if we have entries
		if (requestsBeingProcessed > 0) {

			// calculate maximum limitation
			// and assign it
			uploadProcessingOperationManager.getMasterProcessingOperation().setDownloadAllowanceForIteration(
					uploadProcessingConfigurationManager.getMaximumOverAllRateInKiloBytes() * 1024 / NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND);

			// for all pending operation
			for (Entry<UUID, UploadProcessingOperation> entry : uploadProcessingOperationManager.getClientsAndRequestsProcessingOperation()
					.entrySet()) {

				// process
				processEntry(entry);

			}

		}
	}


	private void processEntry(Entry<UUID, UploadProcessingOperation> entry) {

		// default per request is set to the maximum, so basically maximum by client
		long allowedCapacityPerSecond = uploadProcessingConfigurationManager.getMaximumRatePerClientInKiloBytes() * 1024;

		// extract the configuration element
		final RequestUploadProcessingConfiguration requestUploadProcessingConfiguration =
				uploadProcessingConfigurationManager.getUploadProcessingConfiguration(entry.getKey());

		// calculate from the rate in the configuration
		Long rateInKiloBytes = requestUploadProcessingConfiguration.getRateInKiloBytes();
		if (rateInKiloBytes != null) {
			allowedCapacityPerSecond = (int) (rateInKiloBytes * 1024);
		}

		// calculate statistics
		final long instantRateInBytes = entry.getValue().getAndResetBytesWritten();
		requestUploadProcessingConfiguration.setInstantRateInBytes(instantRateInBytes);

		// calculate what we can write per iteration
		final long allowedCapacityPerIteration = allowedCapacityPerSecond / NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND;

		// set it to the rate conf element
		entry.getValue().setDownloadAllowanceForIteration(allowedCapacityPerIteration);

		log.trace("giving an allowance of " + allowedCapacityPerIteration + " bytes to " + entry.getKey() + ". (consumed " +
				instantRateInBytes + " bytes during previous iteration)");

	}

}
