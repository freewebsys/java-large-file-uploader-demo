package com.am.jlfu.fileuploader.limiter;


import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.am.jlfu.notifier.JLFUListenerPropagator;
import com.am.jlfu.staticstate.JavaLargeFileUploaderService;
import com.am.jlfu.staticstate.StaticStateManager;
import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;



@Component
@ManagedResource(objectName = "JavaLargeFileUploader:name=rateLimiterConfiguration")
public class RateLimiterConfigurationManager
{

	private static final Logger log = LoggerFactory.getLogger(RateLimiterConfigurationManager.class);

	/** Client is evicted from the map when not accessed for that duration */
	@Value("jlfu{jlfu.rateLimiterConfiguration.evictionTimeInSeconds:120}")
	public int clientEvictionTimeInSeconds;

	@Autowired
	private JLFUListenerPropagator jlfuListenerPropagator;

	@Autowired
	private StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;

	@Autowired
	private JavaLargeFileUploaderService<StaticStatePersistedOnFileSystemEntity> staticStateManagerService;

	/** the cache containing all configuration for requests and clients */
	LoadingCache<UUID, RequestUploadProcessingConfiguration> configurationMap;



	@PostConstruct
	private void initMap() {
		configurationMap = CacheBuilder.newBuilder()
				.removalListener(new RemovalListener<UUID, RequestUploadProcessingConfiguration>() {

					@Override
					public void onRemoval(RemovalNotification<UUID, RequestUploadProcessingConfiguration> notification) {
						log.debug("removal from requestconfig of " + notification.getKey() + " because of " + notification.getCause());
						remove(notification.getCause(), notification.getKey());
					}


				})
				.expireAfterAccess(clientEvictionTimeInSeconds, TimeUnit.SECONDS)
				.build(new CacheLoader<UUID, RequestUploadProcessingConfiguration>() {

					@Override
					public RequestUploadProcessingConfiguration load(UUID arg0)
							throws Exception {
						return new RequestUploadProcessingConfiguration();
					}
				});
	}



	private UploadProcessingConfiguration masterProcessingConfiguration = new UploadProcessingConfiguration();

	// ///////////////
	// Configuration//
	// ///////////////

	// 10mb/s
	@Value("jlfu{jlfu.ratelimiter.maximumRatePerClientInKiloBytes:10240}")
	private volatile long maximumRatePerClientInKiloBytes;


	// 10mb/s
	@Value("jlfu{jlfu.ratelimiter.maximumOverAllRateInKiloBytes:10240}")
	private volatile long maximumOverAllRateInKiloBytes;



	// ///////////////


	void remove(RemovalCause cause, UUID key) {

		// if expired
		if (cause.equals(RemovalCause.EXPIRED)) {

			// check if client id is in state
			final StaticStatePersistedOnFileSystemEntity entityIfPresentWithIdentifier =
					staticStateManagerService.getEntityIfPresent(key);

			if (entityIfPresentWithIdentifier != null) {

				// if one of the file is not complete, it is not a natural removal
				// but a
				// timeout!! we propagate the event
				for (Entry<UUID, StaticFileState> lavds : entityIfPresentWithIdentifier.getFileStates().entrySet()) {
					if (!lavds.getValue().getStaticFileStateJson().getCrcedBytes().equals(lavds.getValue().getStaticFileStateJson()
							.getOriginalFileSizeInBytes())) {
						log.debug("inactivity detected for client " + key);
						jlfuListenerPropagator.getPropagator().onClientInactivity(key, clientEvictionTimeInSeconds);
						return;
					}
				}
				log.debug("natural removal for client " + key);

			}
		}
	}


	public Set<Entry<UUID, RequestUploadProcessingConfiguration>> getRequestEntries() {
		return configurationMap.asMap().entrySet();
	}


	public void reset(UUID fileId) {
		final RequestUploadProcessingConfiguration unchecked = configurationMap.getUnchecked(fileId);
		unchecked.setProcessing(false);
	}


	public void assignRateToRequest(UUID fileId, Long rateInKiloBytes) {
		configurationMap.getUnchecked(fileId).rateInKiloBytes = rateInKiloBytes;
	}


	public Long getUploadState(UUID requestIdentifier) {
		return configurationMap.getUnchecked(requestIdentifier).getInstantRateInBytes();
	}


	public RequestUploadProcessingConfiguration getUploadProcessingConfiguration(UUID uuid) {
		return configurationMap.getUnchecked(uuid);
	}


	@ManagedAttribute
	public long getMaximumRatePerClientInKiloBytes() {
		return maximumRatePerClientInKiloBytes;
	}


	@ManagedAttribute
	public void setMaximumRatePerClientInKiloBytes(long maximumRatePerClientInKiloBytes) {
		this.maximumRatePerClientInKiloBytes = maximumRatePerClientInKiloBytes;
	}


	@ManagedAttribute
	public long getMaximumOverAllRateInKiloBytes() {
		return maximumOverAllRateInKiloBytes;
	}


	@ManagedAttribute
	public void setMaximumOverAllRateInKiloBytes(long maximumOverAllRateInKiloBytes) {
		this.maximumOverAllRateInKiloBytes = maximumOverAllRateInKiloBytes;
	}


	public UploadProcessingConfiguration getMasterProcessingConfiguration() {
		return masterProcessingConfiguration;
	}


	public void setClientEvictionTimeInSeconds(int clientEvictionTimeInSeconds) {
		this.clientEvictionTimeInSeconds = clientEvictionTimeInSeconds;
	}


}
