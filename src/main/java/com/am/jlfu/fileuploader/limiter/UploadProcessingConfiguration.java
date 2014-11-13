package com.am.jlfu.fileuploader.limiter;


public class UploadProcessingConfiguration {


	/**
	 * The desired upload rate. <br>
	 * Can be null (the maxmimum rate is applied).
	 */
	volatile Long rateInKiloBytes;

	/**
	 * The statistics.
	 * 
	 * @return
	 */
	long instantRateInBytes;
	int instantRateInBytesCounter;
	Object instantRateInBytesLock = new Object();



	public Long getRateInKiloBytes() {
		return rateInKiloBytes;
	}


	void setInstantRateInBytes(long instantRateInBytes) {
		synchronized (instantRateInBytesLock) {
			this.instantRateInBytesCounter++;
			this.instantRateInBytes += instantRateInBytes;
		}
	}


	long getInstantRateInBytes() {
		int returnValue = 0;
		synchronized (instantRateInBytesLock) {
			if (instantRateInBytesCounter > 0) {
				returnValue = ((int) instantRateInBytes / instantRateInBytesCounter) * RateLimiter.NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND;

				// reset every second or so
				if (instantRateInBytesCounter > RateLimiter.NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND) {
					instantRateInBytes = instantRateInBytesCounter = 0;
				}
			}
		}
		return returnValue;
	}


}
