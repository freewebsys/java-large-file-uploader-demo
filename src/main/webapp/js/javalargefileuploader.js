/*
 * Constructor 
 */
function JavaLargeFileUploader() {
	var globalServletMapping = "javaLargeFileUploaderServlet";
	var uploadServletMapping = "javaLargeFileUploaderAsyncServlet";
	var pendingFiles = new Object();
	var bytesPerChunk;

	javaLargeFileUploaderHost = "";
	progressPollerRefreshRate = 1000;
	maxNumberOfConcurrentUploads = 5; 
	autoRetry = true; 
	autoRetryDelay = 5000; 
	errorMessages = new Object();
	errorMessages[0] = "Request failed for an unknown reason, please contact an administrator if the problem persists.";
	errorMessages[1] = "The request is not multipart.";
	errorMessages[2] = "No file to upload found in the request.";
	errorMessages[3] = "CRC32 Validation of the part failed.";
	errorMessages[4] = "The request cannot be processed because a parameter is missing.";
	errorMessages[5] = "Cannot retrieve the configuration.";
	errorMessages[6] = "No files have been selected, please select at least one file!";
	errorMessages[7] = "Resuming file upload with previous slice as the last part is invalid.";
	errorMessages[8] = "Error while uploading a slice of the file";
	errorMessages[9] = "Maximum number of concurrent uploads reached, the upload is queued and waiting for one to finish.";
	errorMessages[10] = "An exception occurred. Retrying ...";
	errorMessages[11] = "Connection lost. Automatically retrying in a moment.";
	errorMessages[12] = "You do not have the permission to perform this action.";
	errorMessages[13] = "FireBug is enabled, you may experience issues if you do not disable it while uploading.";
	errorMessages[14] = "File corrupted. An unknown error has occured and the file is corrupted. The usual cause is that the file has been modified during the upload. Please clear it and re-upload it.";
	errorMessages[15] = "File is currently locked, retrying in a moment...";
	errorMessages[16] = "Uploads are momentarily disabled, retrying in a moment...";
	exceptionsRetryable = [0,3,7,8,10,11,15,16];
	
	this.setJavaLargeFileUploaderHost = function (javaLargeFileUploaderHostI) {
		javaLargeFileUploaderHost = javaLargeFileUploaderHostI;
	};
	
	this.setMaxNumberOfConcurrentUploads = function (maxNumberOfConcurrentUploadsI) {
		maxNumberOfConcurrentUploads = maxNumberOfConcurrentUploadsI;
	};

	this.getErrorMessages = function () {
		return errorMessages;
	};
	
	this.setProgressPollerRefreshRate = function(progressPollerRefreshRateI) {
		progressPollerRefreshRate = progressPollerRefreshRateI;
	};
	
	this.setAutoRetry = function (autoRetryBoolean, autoRetryDelayI) {
		autoRetry = autoRetryBoolean;
		autoRetryDelay = autoRetryDelayI;
	};
	
	this.initialize = function (initializationCallback, exceptionCallback, optionalClientOrJobIdentifier) {
		
		//if an id is specified
		var appended = "";
		if (optionalClientOrJobIdentifier) {
			appended = "&clientId="+optionalClientOrJobIdentifier;
		}
		
		//if firebug is enabled, show exception
		manageFirebug(exceptionCallback);
		
		// get the configuration
		$.get(javaLargeFileUploaderHost + globalServletMapping + "?action=getConfig" + appended, function(data) {
			if (data) {
				bytesPerChunk = data.inByte;
	
				// adjust values to display
				if (!jQuery.isEmptyObject(data.pendingFiles)) {
					pendingFiles = data.pendingFiles;
					$.each(data.pendingFiles, function(key, pendingFile) {
						pendingFile.id = key;
						pendingFile.fileCompletion = getFormattedSize(pendingFile.fileCompletionInBytes);
						pendingFile.originalFileSize = getFormattedSize(pendingFile.originalFileSizeInBytes);
						pendingFile.percentageCompleted = format(pendingFile.fileCompletionInBytes * 100 / pendingFile.originalFileSizeInBytes);
						pendingFile.started = false;
					});
				}
				initializationCallback(pendingFiles);
	
			} else {
				if (exceptionCallback) {
					//cannot retrieve the configuration
					exceptionCallback(errorMessages[5]);
				}
			}
		});
		
		// launch the progress poller
		startProgressPoller();
		
		//initialize the pause all file uploads on close
		$(window).bind('unload', function() {
			pauseAllFileUploadsI(false);
		});

		
	};
	
	this.clearFileUpload = function (callback) {
		$.each(pendingFiles, function(key, pendingFile) {
			pendingFile.cancelled = true;
		});
		pendingFiles = new Object();
		$.get(javaLargeFileUploaderHost + globalServletMapping + "?action=clearAll", function(e) {
			if (callback) {
				callback();
			}
		});
	};
	
	this.setRateInKiloBytes = function (fileId, rate) {
		if(fileId && rate)  {
			$.get(javaLargeFileUploaderHost + globalServletMapping + "?action=setRate&rate="+rate+"&fileId="+fileId);
		}
	};
	
	this.cancelFileUpload = function (fileIdI, callback) {
		var fileId = fileIdI;
		if(fileId && pendingFiles[fileId]) {
			pendingFiles[fileId].cancelled = true;
			$.get(javaLargeFileUploaderHost + globalServletMapping + "?action=clearFile&fileId=" + fileId,	function(e) {
				abort(pendingFiles[fileId], false);
				if (callback) {
					callback(fileId);
				}
				delete pendingFiles[fileId];
				processNextInQueue();
			});
		}
	};
	
	this.pauseFileUpload = function (fileIdI, callback) {
		pauseFileUploads(true, [fileIdI], callback);
	};
	
	this.pauseAllFileUploads = function (callback) {
		pauseAllFileUploadsI(true, callback);
	};
	
	function pauseAllFileUploadsI(async, callback) {
		var fileIds = [];
		for (var fileId in pendingFiles) {
			fileIds.push(fileId);
		}
		pauseFileUploads(async, fileIds, callback);  
	}
	
	function pauseFileUploads(async, fileIds, callback) {
		var filesToSend = []; 
		for (var i in fileIds) {
	        var fileId = fileIds[i];
			if(fileId && pendingFiles[fileId] && isFilePaused(pendingFiles[fileId]) === false  && pendingFiles[fileId].resuming === false) {
				if (pendingFiles[fileId].queued === true) {
					pendingFiles[fileId].paused = true;
				} else {
					pendingFiles[fileId].pausing = true;
					filesToSend.push(fileId);
				}
			}
		}
		if (filesToSend.length > 0) {
			$.ajax({
				  url: javaLargeFileUploaderHost + globalServletMapping + "?action=pauseFile&fileId=" + filesToSend,
				  success: function() {
						for (var i in fileIds) {
							var fileId = fileIds[i];
							var pendingFile = pendingFiles[fileId];
							pendingFile.pausedCallback = callback;
							abort(pendingFile, true);
						}
					},
					async: async
				});
		}
	}
	
	function abort(pendingFile, forPauseBool) {
		if (pendingFile.xhr) {
			pendingFile.xhr.abort();
		}
		if (forPauseBool) {
			setTimeout(function() {
				//if still paused after a certain delay, we unblock it
				if (pendingFile.paused===false) {
					notifyPause(pendingFile);
				}
			}, 2000);
		}
	}
	
	function notifyPause(pendingFile) {
		if (pendingFile.pausing) {
			console.log("The file is paused.");
			uploadEnd(pendingFile, false);
			pendingFile.paused=true;
			if (pendingFile.pausedCallback) {
				pendingFile.pausedCallback(pendingFile);
			}
		}
	}
	
	function isFilePaused(pendingFile) {
		return pendingFile.paused || pendingFile.pausing;
	}
	
	this.resumeFileUpload = function (fileIdI, callback) {
		this.resumeFileUploads([fileIdI], callback);
	};
	
	this.resumeAllFileUploads = function(callback) {
		var fileIds = [];
		for (var fileId in pendingFiles) {
			fileIds.push(fileId);
		}
		this.resumeFileUploads (fileIds, callback);  
	};
	
	this.resumeFileUploads = function (fileIds, callback) {
		for (var i in fileIds) {
			var fileIdI = fileIds[i];
			if (fileIdI && pendingFiles[fileIdI]) {
				if (pendingFiles[fileIdI].paused === true && pendingFiles[fileIdI].resuming === false) {
					pendingFiles[fileIdI].resuming = true;
					resumeFileUploadInternal(pendingFiles[fileIdI], callback);
				}
			}
		}
	};
	
	this.retryFileUpload = function (fileIdI, callback) {
		if (fileIdI && pendingFiles[fileIdI]) {
			resumeFileUploadInternal(pendingFiles[fileIdI], null, callback);
		}
	};
	
	function displayException(pendingFile, errorMessageId) {
		console.log(errorMessages[errorMessageId]);
		if(pendingFile.exceptionCallback) {
			pendingFile.exceptionCallback(errorMessages[errorMessageId], pendingFile.referenceToFileElement, pendingFile);
		}
	}
	
	function retryStart(pendingFile) {
		setTimeout(retryRecursive, autoRetryDelay, pendingFile);
	}
	
	function retryRecursive(pendingFile) {
		if (pendingFile) {
			displayException(pendingFile, 10);
			resumeFileUploadInternal(pendingFile, null, function(ok) {
				if (ok === false) {
					displayException(pendingFile, 11);
					retryStart(pendingFile);
				}
			});
		}
	}
	
	function resumeFileUploadInternal(pendingFile, callback, retryCallback) {
		if(pendingFile) {
			//and restart flow
			$.get(javaLargeFileUploaderHost + globalServletMapping + "?action=resumeFile&fileId=" + pendingFile.id,	function(data) {
				if (callback) {
					callback(pendingFile);
				}
					
				//populate crc data
				pendingFile.crcedBytes = data.crcedBytes;
				pendingFile.fileCompletionInBytes = data.fileCompletionInBytes;
					
				//try to validate the unvalidated chunks and resume it
				fileResumeProcessStarter(pendingFile);
			}).success(function(e) {
				if (retryCallback) {
					retryCallback(true);
				}
			}).error(function(e) {
				if (retryCallback) {
					retryCallback(false);
				}
			});
		}
	}
	
	this.fileUploadProcess = function (referenceToFileElement, startCallback, progressCallback,
			finishCallback, exceptionCallback) {

		//read the file information from input
		var allFiles = extractFilesInformation(referenceToFileElement, startCallback, progressCallback,
				finishCallback, exceptionCallback);
		//copy it to another array which is gonna contain the new files to process
		var potentialNewFiles = allFiles.slice(0);

		//try to corrolate information with our pending files
		//corrolate with filename  size and crc of first chunk
		//start resuming if we have a match
		//if we dont have any name/size math, we process an upload 
		var potentialResumeCounter = new Object();
		potentialResumeCounter.counter = 0;
		for (fileKey in allFiles) {
			var pendingFile = allFiles[fileKey];
				
			//look for a match in the pending files
			for (pendingFileToCheckKey in pendingFiles) {
				var pendingFileToCheck = pendingFiles[pendingFileToCheckKey];
				
				if (pendingFileToCheck.originalFileName == pendingFile.originalFileName && 
						pendingFileToCheck.originalFileSizeInBytes == pendingFile.originalFileSizeInBytes) {
					
					//we might have a match, adding a match counter entry
					potentialResumeCounter.counter++;
					
					//check the crc first slice 
					//that method will take care of the new files processing when all complete
					processCrcFirstSlice(potentialNewFiles, pendingFile, pendingFileToCheck, potentialResumeCounter);
				} 
			}
		}
		
		//process if no pending to resume
		if (potentialResumeCounter.counter === 0 && potentialNewFiles.length > 0) {
			processNewFiles(potentialNewFiles);
		}
		
	};

	function extractCrcFirstSlice(blob, callback) {
		
		//calculate the slice
		var end = 8192;
		if (blob.size < 8192) {
			end = blob.size;
		}
		
		var reader = new FileReader();
		reader.onloadend = function(e) {
			//if the read is complete
			if (e.target.readyState == FileReader.DONE) { // DONE == 2
				callback(decimalToHexString(crc32(e.target.result)), blob);
			}
		};
		reader.readAsBinaryString(slice(blob, 0, end));

	}
	
	
	
	function processCrcFirstSlice(potentialNewFiles, pendingFileI, pendingFileToCheckI, potentialResumeCounter){
		
		// prepare the checksum of the slice
		var pendingFile = pendingFileI;
		var pendingFileToCheck = pendingFileToCheckI;
		extractCrcFirstSlice(pendingFile.blob, function(crc, blob) {
			
	    	//if that pendingfile is still there
	    	if (potentialNewFiles.indexOf(pendingFile) != -1) {
	    	
		        //calculate crc of the chunk read
		        //compare it 
		    	//if it is the correct file
		    	//proceed
		        if (crc == pendingFileToCheck.firstChunkCrc) {
					
		        	//remove it from new file ids (as we are now sure it is not a new file)
		        	potentialNewFiles.splice(potentialNewFiles.indexOf(pendingFile), 1);
					
		        	//if that file is not already being uploaded:
		        	if (!pendingFileToCheck.started) {

		        		//if that file is paused 
		        		if (isFilePaused(pendingFileToCheck)) {
		        		
		        			//resume it
		        			resumeFileUploadInternal(pendingFileToCheck);

		        		} else {
		        			
		        			//fill pending file to check with new info
		        			//populate stuff retrieved in initialization 
		        			pendingFile.fileCompletionInBytes = pendingFileToCheck.fileCompletionInBytes;
		        			pendingFile.crcedBytes = pendingFileToCheck.crcedBytes;
		        			pendingFile.firstChunkCrc = pendingFileToCheck.firstChunkCrc;
		        			pendingFile.started = pendingFileToCheck.started;
		        			pendingFile.id = pendingFileToCheck.id;
		        			
		        			//put it into the pending files array
		        			pendingFiles[pendingFileToCheck.id] = pendingFile;
		        			
		        			// process the upload
		        			fileResumeProcessStarter(pendingFile);
		        		}
					}
		        	
		        	
				} else {
					console.log("Invalid resume crc for "+pendingFileToCheck.originalFileName+". processing as a new file.");
				}
		        
		        //if its not the correct file, it will be processed in processNewFiles
		        
		        //decrement potential resume counter
		        potentialResumeCounter.counter--;
		        
		        //and if it was the last one, process the new files.
		        if (potentialResumeCounter.counter === 0 && potentialNewFiles.length > 0) {
		        	processNewFiles(potentialNewFiles);
		        }

	    	}
		});
	}
	
	
	
    function extractFilesInformation(referenceToFileElements, startCallback, progressCallback, finishCallback, exceptionCallback){
    	var retArray = [];

    	if($.isArray(referenceToFileElements)) {
    		for (var i = 0 ; i < referenceToFileElements.length; i++){
    			retArray = retArray.concat(extractSingleElementFilesInformationProcess(referenceToFileElements[i], startCallback, progressCallback, finishCallback, exceptionCallback));
    		}
    	} else {
    		retArray = extractSingleElementFilesInformationProcess(referenceToFileElements, startCallback, progressCallback, finishCallback, exceptionCallback);
    	}
        return retArray;
    }
	
	
	
	function extractSingleElementFilesInformationProcess(referenceToFileElement, startCallback, progressCallback,
			finishCallback, exceptionCallback) {
		var newFiles = [];
		
		//extract files
		var files = referenceToFileElement.files;
		if (!files.length) {
			if (exceptionCallback) {
				//no file selected
				console.log(errorMessages[6]);
				exceptionCallback(errorMessages[6], referenceToFileElement);
			}
		} else {
			for (fileKey in files) {
				var file = files[fileKey];
				if (file.name && file.size) {
					
					//init the pending file object
					var pendingFile = new Object();
					pendingFile.originalFileName = file.name; 
					pendingFile.originalFileSizeInBytes = file.size;
					pendingFile.originalFileSize = getFormattedSize(pendingFile.originalFileSizeInBytes);
					pendingFile.blob = file;  
					pendingFile.progressCallback=progressCallback;
					pendingFile.referenceToFileElement= referenceToFileElement;
					pendingFile.startCallback= startCallback;
					pendingFile.finishCallback= finishCallback;
					pendingFile.exceptionCallback= exceptionCallback;
					pendingFile.paused=false;
					pendingFile.pausing=false;
					pendingFile.resuming = false;
					
					//put it into the temporary new file array as every file is potentially a new file until it is proven it is not a new file
					newFiles.push(pendingFile);
				}
			}
		}
		
		return newFiles;
	}
	
	function processNewFiles(newFiles) {
		
		//for the new files left, prepare initiation
		var jsonVersionOfNewFiles = [];
		var newFilesIds = 0;
		var crcsCalculated = 0;
		for (pendingFileId in newFiles) {
			var pendingFile = newFiles[pendingFileId];
			
			// prepare the objects
			var fileForPost = new Object();
			fileForPost.tempId=newFilesIds;
			fileForPost.fileName=pendingFile.originalFileName;
			fileForPost.size=pendingFile.originalFileSizeInBytes;
			jsonVersionOfNewFiles[fileForPost.tempId]=fileForPost;
			pendingFiles[fileForPost.tempId]=pendingFile;
			newFilesIds++;

			//extract first chunk crc
			pendingFile.blob.i = fileForPost.tempId;
			extractCrcFirstSlice(pendingFile.blob, function(crc, blob) {
				jsonVersionOfNewFiles[blob.i].crc = crc;
				pendingFiles[blob.i].firstChunkCrc=crc;
				crcsCalculated++;
				if (crcsCalculated == jsonVersionOfNewFiles.length) {
					$.getJSON(javaLargeFileUploaderHost + globalServletMapping + "?action=prepareUpload", {newFiles: JSON.stringify(jsonVersionOfNewFiles)}, function(data) {
						
						//now populate our local entries with ids
						$.each(data , function(tempIdI, fileIdI) {
							
							//now that we have the file id, we can assign the object
							fileId = fileIdI;
							pendingFile = pendingFiles[tempIdI];
							pendingFile.id = fileId;
							pendingFile.fileComplete = false;
							pendingFile.fileCompletionInBytes = 0;
							pendingFiles[fileId] = pendingFile;
							delete pendingFiles[tempIdI];
							
							//call callback
							if (pendingFile.startCallback) {
								pendingFile.startCallback(pendingFile, pendingFile.referenceToFileElement);
							}
							
							// and process the upload
							fileUploadProcessStarter(pendingFile);
						});
					});
				}
			});
			
		}
	}
	
	function fileResumeProcessStarter(pendingFile) {
		
	      //we have to ensure that the last chunk update that have not been validated is correct
		var bytesToValidates = pendingFile.fileCompletionInBytes - pendingFile.crcedBytes;
		
		//if we have bytes to validate
		if (bytesToValidates > 0) {
			
			//slice the not validated part
			var chunk = slice(pendingFile.blob, pendingFile.crcedBytes , pendingFile.fileCompletionInBytes);
			
			//append chunk to a formdata
			var formData = new FormData();
			formData.append("file", chunk);
		
			// prepare the checksum of the slice
			var reader = new FileReader();
			reader.onloadend = function(e) {
			    if (e.target.readyState == FileReader.DONE) { // DONE == 2
					//calculate crc of the chunk read
			        var digest = crc32(e.target.result);

			        //and send it
					$.get(javaLargeFileUploaderHost + globalServletMapping + "?action=verifyCrcOfUncheckedPart&fileId=" + pendingFile.id + "&crc=" + decimalToHexString(digest),	function(data) {
						//check if we have an exception
						if (data.value) {
							displayException(pendingFile, data.value);
							if (autoRetry && isExceptionRetryable(data.value)) {
								//submit retry
								retryStart(pendingFile);
							}
						} else {
							//verify stuff!
							if (data === false) {
								displayException(pendingFile, 7);
								console.log("crc verification failed for unchecked chunk, filecompletion is truncated to "+pendingFile.crcedBytes+" (was "+pendingFile.fileCompletionInBytes+")");
								//and assign the completion to last verified
								pendingFile.fileCompletionInBytes = pendingFile.crcedBytes;
							}	
							//then process upload
							fileUploadProcessStarter(pendingFile);						
						}
					});
			    }
				
			};
			//read the chunk to calculate the crc
			reader.readAsBinaryString(chunk);
			
		} 
		//if we dont have bytes to validate, process
		else {
			
			//if everything is good, resume it:
			fileUploadProcessStarter(pendingFile);

		}
		
		
	}
	
	function canUploadBeProcessed() {
		var numberOfUploadsCurrentlyBeingProcessed = 0;
		for(fileId in pendingFiles) {
			var pendingFile = pendingFiles[fileId];
			if (pendingFile.started ) {
				numberOfUploadsCurrentlyBeingProcessed++;
			}
		}
		//we can process only if we are under the capacity
		return numberOfUploadsCurrentlyBeingProcessed < maxNumberOfConcurrentUploads;
	}
	
	function fileUploadProcessStarter(pendingFile) {
		
		//if the file is not complete
		if (pendingFile.fileCompletionInBytes < pendingFile.originalFileSizeInBytes) {

			//reset some tags
			pendingFile.paused = false;
			pendingFile.pausing = false;
			pendingFile.resuming = false;
			
			//check if we can process the upload
			if (canUploadBeProcessed() === true) {
				
				
				// start
				pendingFile.end = pendingFile.fileCompletionInBytes + bytesPerChunk;
				pendingFile.started = true;
				pendingFile.queued = false;
				
				console.log("processing "+pendingFile.id+" for slice "+pendingFile.fileCompletionInBytes + " - "+pendingFile.end);

				// then process the recursive function
				go(pendingFile);

			} else {
				//queue it
				pendingFile.queued = true;
				
				//specify to user
				displayException(pendingFile, 9);
			}
			
		} 
		//otherwise
		else {
			//mark it as complete
			pendingFile.fileComplete=true;
		}
		
		
	
	}
	
	function slice(blob, start, end) {
		
		if (blob.slice) {
			return blob.slice(start, end);
		} else if (blob.mozSlice) {
			return blob.mozSlice(start, end);
		} else {
			return blob.webkitSlice(start, end);
		}
	}
	
	
	function go(pendingFile) {
		
		//every time a chunk is being uplodaed, we check for firebug !
		manageFirebug(pendingFile.exceptionCallback);
	
		//if file id is in the pending files:
		var chunk = slice(pendingFile.blob, pendingFile.fileCompletionInBytes, pendingFile.end);
	
		//append chunk to a formdata
		var formData = new FormData();
		formData.append("file", chunk);
	
		// prepare the checksum of the slice
		var reader = new FileReader();
		reader.onloadend = function(e) {
		    if (e.target.readyState == FileReader.DONE) { // DONE == 2
				//calculate crc of the chunk read
		        var digest = crc32(e.target.result);
		
				// prepare xhr request
				var xhr = new XMLHttpRequest();
				pendingFile.xhr = xhr;
				
				//assign pause callback
				xhr.addEventListener("abort", function(event) {
					notifyPause(pendingFile);
				}, false);
				
				//then open
				xhr.open('POST', javaLargeFileUploaderHost + uploadServletMapping + '?action=upload&fileId=' + pendingFile.id + '&crc=' + decimalToHexString(digest), true);
		
				// assign callback
				xhr.onreadystatechange = function() {
					if (xhr.readyState == 4) {
		
						//if we are pausing or cancelling, we just return
						if (pendingFile.pausing || pendingFile.cancelled) {
							return;
						}
						
						//if we have an exception in the call
						if (xhr.status != 200) {
							displayException(pendingFile, 8);
							if (autoRetry) {
								//submit retry
								retryStart(pendingFile);
							}
							uploadEnd(pendingFile, true);
							return;
						}
						
						//if we have an exception in the response text
						if (xhr.response) {
							var resp = JSON.parse(xhr.response);
							displayException(pendingFile, resp.value);
							if (autoRetry && isExceptionRetryable(resp.value)) {
								//submit retry
								retryStart(pendingFile);
							}
							uploadEnd(pendingFile, true);
							return;
						}
		
						// progress
						pendingFile.fileCompletionInBytes = pendingFile.end;
						pendingFile.end = pendingFile.fileCompletionInBytes + bytesPerChunk;
		
						// check if we need to go on
						if (pendingFile.fileCompletionInBytes < pendingFile.originalFileSizeInBytes) {
							// recursive call
							setTimeout(go, 5, pendingFile);
						} else {
							pendingFile.fileComplete=true;
							uploadEnd(pendingFile, false);
							// finish callback
							if (pendingFile.finishCallback) {
								pendingFile.finishCallback(pendingFile, pendingFile.referenceToFileElement);
							}
						}
					}
				};
		
				// send xhr request
				try {
					//only send if it is pending, because it could have been asked for cancellation while we were reading the file!
					if (pendingFiles[pendingFile.id]) {
						//and if we are not pausing or cancelling
						if (!isFilePaused(pendingFile) && !pendingFile.cancelled) {
							xhr.send(formData);
						}
					}
				} catch (e) {
					uploadEnd(pendingFile, true);
					displayException(pendingFile, 8);
					if (autoRetry) {
						//submit retry
						retryStart(pendingFile);
					}
					return;
				}
		    }
			
		};
		//read the chunk to calculate the crc
		reader.readAsBinaryString(chunk);

	
	}
	
	function uploadEnd(pendingFile, withException) {
		
		//the file is not started anymore
		pendingFile.started=false;
		
		//process the queue if it was not an exception and if there is no auto retry
		if (withException === false) {
			processNextInQueue();
		}
	}
	
	function processNextInQueue() {
		for(fileId in pendingFiles) {
			if (pendingFiles[fileId].queued && !pendingFiles[fileId].paused) {
				fileUploadProcessStarter(pendingFiles[fileId]);
				return;
			}
		}
	}
	

	/*
	 * inspired from http://codeaid.net/javascript/convert-seconds-to-hours-minutes-and-seconds-(javascript)
	 */
	function getFormattedTime(secs)
	{
		if (secs < 1) {
			return "-";
		}
		
	    var hours = Math.floor(secs / (60 * 60));
	    
	    var divisor_for_minutes = secs % (60 * 60);
	    var minutes = Math.floor(divisor_for_minutes / 60);
	 
	    var divisor_for_seconds = divisor_for_minutes % 60;
	    var seconds = Math.ceil(divisor_for_seconds);
	    
	    var returned = '';
	    var displaySeconds = true;
	    if (hours > 0) {
	    	returned += hours + "h";
	    	displaySeconds = false;
	    }
	    if (minutes > 0) {
	    	returned += minutes + "m";
	    	displaySeconds &= minutes <= 10;
	    }
	    if (displaySeconds) {
	    	returned += seconds + "s";
	    }
	    return returned;
	}
	
	function getFormattedSize(size) {
		if (size < 1024) {
			return format(size) + 'B';
		} else if (size < 1048576) {
			return format(size / 1024) + 'KB';
		} else if (size < 1073741824) {
			return format(size / 1048576) + 'MB';
		} else if (size < 1099511627776) {
			return format(size / 1073741824) + 'GB';
		} else if (size < 1125899906842624) {
			return format(size / 1099511627776) + 'TB';
		}
	}
	
	function format(size) {
		return Math.ceil(size*100)/100;
	}
	
	function uploadIsActive(pendingFile) {
		//process only if we have this id in the pending files and if the file is incomplete and if the file is not paused and if the file is started!
		return pendingFile && pendingFiles[pendingFile.id] && !isFilePaused(pendingFile) && !pendingFile.fileComplete && pendingFile.started;
	}
	
	function isExceptionRetryable(errorId) {
		return (exceptionsRetryable.indexOf(parseInt(errorId)) != -1);
	}

	function manageFirebug(exceptionCallback) {
		//if firebug is enabled, show exception
		if (window.console && (window.console.firebug || window.console.exception)) {
			if (exceptionCallback) {
				exceptionCallback(errorMessages[13]);
			} else {
				alert(errorMessages[13]);
			}
		}		
	}
	
	function startProgressPoller() {
		
		//first fill the request array
		var fileIds = [];
		
		//for all the pending files
		for (fileId in pendingFiles) {
			var pendingFile = pendingFiles[fileId];
	
			//if active
			//and if we have a progress listener
			if(uploadIsActive(pendingFile) && pendingFile.progressCallback) {
				fileIds.push(fileId);
			}
			
		}
			
		if (fileIds.length > 0) {
				  $.getJSON(javaLargeFileUploaderHost + globalServletMapping + "?action=getProgress", {fileId: JSON.stringify(fileIds)}, function(data) {
					  
					  //now populate our local entries with ids
					  $.each(data, function(fileId, progress) {
						  	var pendingFile = pendingFiles[fileId];
						
						  	//if the pending file status has not been deleted while we querying:
							if(uploadIsActive(pendingFile)) {
	
								//if we have information about the rate:
								if (progress.uploadRate != undefined) {
									var uploadRate = getFormattedSize(progress.uploadRate);
								}
								
								//if we have information about the time remaining:
								if (progress.estimatedRemainingTimeInSeconds != undefined) {
									var estimatedRemainingTimeInSeconds = getFormattedTime(progress.estimatedRemainingTimeInSeconds);
								}
								
								//keep progress
								pendingFile.percentageCompleted = format(progress.progress);
								
								// specify progress
								pendingFile.progressCallback(pendingFile, pendingFile.percentageCompleted, uploadRate, estimatedRemainingTimeInSeconds,
										pendingFile.referenceToFileElement);
								
							}
					  });
				  }).complete(function() {
					  
					  //reschedule when the have the answer 
					  setTimeout(startProgressPoller, progressPollerRefreshRate);
				  });
		} 
		//reschedule immediately if there is no pending upload
		else {
			setTimeout(startProgressPoller, progressPollerRefreshRate);
		}


	}
	
	
	/*  
	===============================================================================
	Crc32 is a JavaScript function for computing the CRC32 of a string
	...............................................................................
	
	Version: 1.2 - 2006/11 - http://noteslog.com/post/crc32-for-javascript/
	
	-------------------------------------------------------------------------------
	Copyright (c) 2006 Andrea Ercolino      
	http://www.opensource.org/licenses/mit-license.php
	===============================================================================
	*/
	
	var strTable = "00000000 77073096 EE0E612C 990951BA 076DC419 706AF48F E963A535 9E6495A3 0EDB8832 79DCB8A4 E0D5E91E 97D2D988 09B64C2B 7EB17CBD E7B82D07 90BF1D91 1DB71064 6AB020F2 F3B97148 84BE41DE 1ADAD47D 6DDDE4EB F4D4B551 83D385C7 136C9856 646BA8C0 FD62F97A 8A65C9EC 14015C4F 63066CD9 FA0F3D63 8D080DF5 3B6E20C8 4C69105E D56041E4 A2677172 3C03E4D1 4B04D447 D20D85FD A50AB56B 35B5A8FA 42B2986C DBBBC9D6 ACBCF940 32D86CE3 45DF5C75 DCD60DCF ABD13D59 26D930AC 51DE003A C8D75180 BFD06116 21B4F4B5 56B3C423 CFBA9599 B8BDA50F 2802B89E 5F058808 C60CD9B2 B10BE924 2F6F7C87 58684C11 C1611DAB B6662D3D 76DC4190 01DB7106 98D220BC EFD5102A 71B18589 06B6B51F 9FBFE4A5 E8B8D433 7807C9A2 0F00F934 9609A88E E10E9818 7F6A0DBB 086D3D2D 91646C97 E6635C01 6B6B51F4 1C6C6162 856530D8 F262004E 6C0695ED 1B01A57B 8208F4C1 F50FC457 65B0D9C6 12B7E950 8BBEB8EA FCB9887C 62DD1DDF 15DA2D49 8CD37CF3 FBD44C65 4DB26158 3AB551CE A3BC0074 D4BB30E2 4ADFA541 3DD895D7 A4D1C46D D3D6F4FB 4369E96A 346ED9FC AD678846 DA60B8D0 44042D73 33031DE5 AA0A4C5F DD0D7CC9 5005713C 270241AA BE0B1010 C90C2086 5768B525 206F85B3 B966D409 CE61E49F 5EDEF90E 29D9C998 B0D09822 C7D7A8B4 59B33D17 2EB40D81 B7BD5C3B C0BA6CAD EDB88320 9ABFB3B6 03B6E20C 74B1D29A EAD54739 9DD277AF 04DB2615 73DC1683 E3630B12 94643B84 0D6D6A3E 7A6A5AA8 E40ECF0B 9309FF9D 0A00AE27 7D079EB1 F00F9344 8708A3D2 1E01F268 6906C2FE F762575D 806567CB 196C3671 6E6B06E7 FED41B76 89D32BE0 10DA7A5A 67DD4ACC F9B9DF6F 8EBEEFF9 17B7BE43 60B08ED5 D6D6A3E8 A1D1937E 38D8C2C4 4FDFF252 D1BB67F1 A6BC5767 3FB506DD 48B2364B D80D2BDA AF0A1B4C 36034AF6 41047A60 DF60EFC3 A867DF55 316E8EEF 4669BE79 CB61B38C BC66831A 256FD2A0 5268E236 CC0C7795 BB0B4703 220216B9 5505262F C5BA3BBE B2BD0B28 2BB45A92 5CB36A04 C2D7FFA7 B5D0CF31 2CD99E8B 5BDEAE1D 9B64C2B0 EC63F226 756AA39C 026D930A 9C0906A9 EB0E363F 72076785 05005713 95BF4A82 E2B87A14 7BB12BAE 0CB61B38 92D28E9B E5D5BE0D 7CDCEFB7 0BDBDF21 86D3D2D4 F1D4E242 68DDB3F8 1FDA836E 81BE16CD F6B9265B 6FB077E1 18B74777 88085AE6 FF0F6A70 66063BCA 11010B5C 8F659EFF F862AE69 616BFFD3 166CCF45 A00AE278 D70DD2EE 4E048354 3903B3C2 A7672661 D06016F7 4969474D 3E6E77DB AED16A4A D9D65ADC 40DF0B66 37D83BF0 A9BCAE53 DEBB9EC5 47B2CF7F 30B5FFE9 BDBDF21C CABAC28A 53B39330 24B4A3A6 BAD03605 CDD70693 54DE5729 23D967BF B3667A2E C4614AB8 5D681B02 2A6F2B94 B40BBE37 C30C8EA1 5A05DF1B 2D02EF8D".split(' ');
	        
    var table = new Array();
    for (var i = 0; i < strTable.length; ++i) {
      table[i] = parseInt("0x" + strTable[i]);
    }

    /* Number */
    function crc32( /* String */ str) {
            var crc = 0;
            var n = 0; //a number between 0 and 255
            var x = 0; //an hex number

            crc = crc ^ (-1);
            for( var i = 0, iTop = str.length; i < iTop; i++ ) {
                    n = ( crc ^ str.charCodeAt( i ) ) & 0xFF;
                    crc = ( crc >>> 8 ) ^ table[n];
            }
            return crc ^ (-1);
    }
	
	function decimalToHexString(number) {
	    if (number < 0) {
	        number = 0xFFFFFFFF + number + 1;
	    }
	
	    return number.toString(16).toLowerCase();
	}
}


