package com.am.jlfu.fileuploader.utils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.am.jlfu.staticstate.entities.FileProgressStatus;
import com.google.common.collect.Maps;

/**
 * Component dedicated to calculate the remaining time of a pending upload.
 * @author antoinem
 */
@Component
public class RemainingTimeEstimator {

	private static final int averageUploadRateOnTheLastX = 10;
	
	private Map<UUID, LimitingList<Long>> map = Maps.newConcurrentMap();

	public Long getRemainingTime(UUID fileId, FileProgressStatus progress, Long uploadRate) {
		LimitingList<Long> newArrayList;
		
		//if we dont have an array for this file yet
		if ((newArrayList = map.get(fileId)) == null) {
			
			//create it and set it
			newArrayList = new LimitingList<Long>(averageUploadRateOnTheLastX);
			map.put(fileId, newArrayList);
			
		}
		
		//add the instant upload rate
		newArrayList.unshift(uploadRate);
		
		//calculate the average upload rate
		Long averageUploadRate = getAverageUploadRate(newArrayList);
		
		//return null if average upload rate is 0
		if (averageUploadRate == 0) {
			return null;
		}
		
		//calculate from average
		return calculateRemainingTime(progress, averageUploadRate);
	}
	
	private Long getAverageUploadRate(LimitingList<Long> newArrayList) {
		Long totalValue = 0l;
		List<Long> list = newArrayList.getList();
		for (Long value : list) {
			totalValue += value;
		}
		return totalValue / list.size();
	}

	long calculateRemainingTime(FileProgressStatus progress, Long uploadRate) {
		long calculatedTimeRemaining = (progress.getTotalFileSize() - progress.getBytesUploaded()) / uploadRate;
		//set the minimum to 1second remaining
		return Math.max(calculatedTimeRemaining, 1);
	}


}
