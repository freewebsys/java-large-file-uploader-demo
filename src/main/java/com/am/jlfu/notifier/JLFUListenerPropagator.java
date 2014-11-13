package com.am.jlfu.notifier;


import org.springframework.stereotype.Component;

import com.am.jlfu.notifier.utils.GenericPropagator;



/**
 * Propagates the events to the registered listeners.
 * 
 * @author antoinem
 * 
 */
@Component
public class JLFUListenerPropagator extends GenericPropagator<JLFUListener> {

	@Override
	protected Class<JLFUListener> getProxiedClass() {
		return JLFUListener.class;
	}

}
