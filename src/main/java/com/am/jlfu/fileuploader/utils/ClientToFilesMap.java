package com.am.jlfu.fileuploader.utils;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

@Component
public class ClientToFilesMap extends ForwardingMap<UUID, Set<UUID>> {

	/** Maps a client to its current requests */
	private final ConcurrentMap<UUID, Set<UUID>> clientToRequestsMapping = Maps.newConcurrentMap();

	@Override
	protected Map<UUID, Set<UUID>> delegate() {
		return clientToRequestsMapping;
	}

}
