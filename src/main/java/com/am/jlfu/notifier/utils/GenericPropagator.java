package com.am.jlfu.notifier.utils;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.am.jlfu.notifier.JLFUListener;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;



/**
 * Propagates the methods called on {@link #proxiedElement} to all objects in {@link #propagateTo}.<br>
 * <strong>{@link #getProxiedClass()} has to be overridden by any subclass.</strong><br>
 * Note that in order to not block the caller, the invocation is processed in a separate thread.
 * 
 * @param <T>
 * @author antoinem
 */
public abstract class GenericPropagator<T> {

	private static final Logger log = LoggerFactory.getLogger(GenericPropagator.class);


	/** The element proxied by {@link #initProxy()} */
	private T proxiedElement;

	/** List of objects to propagate to */
	private List<T> propagateTo = Lists.newArrayList();



	/**
	 * @return The class of {@link #proxiedElement}
	 */
	protected abstract Class<T> getProxiedClass();


	@PostConstruct
	@SuppressWarnings("unchecked")
	private void initProxy() {

		// initialize the proxy
		proxiedElement = (T) Proxy.newProxyInstance(
				getProxiedClass().getClassLoader(),
				new Class[] { getProxiedClass() },
				new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, final Method method, final Object[] args)
							throws Throwable {
						synchronized (propagateTo) {
							process(new ArrayList<T>(propagateTo), method, args);
						}
						return null;
					}


					private void process(final List<T> list, final Method method, final Object[] args) {
						new Thread() {

							@Override
							public void run() {
								
								if (log.isTraceEnabled()) {
									log.trace("{propagating `" + method.getName() + "` with args `"+Joiner.on(',').join(args)+"` to `" + list.size() + "` elements}");
								}
								
								for (T o : list) {
									try {
										method.invoke(o, args);
									}
									catch (Exception e) {
										log.error("cannot propagate " + method.getName(), e);
									}
								}
							}
						}.start();
					}
				});

	}


	/**
	 * Register a propagant to {@link #propagateTo}.
	 * 
	 * @param propagant
	 */
	public void registerListener(T propagant) {
		synchronized (propagateTo) {
			propagateTo.add(propagant);
		}
	}


	/**
	 * Unregister a propagant from {@link #propagateTo}.
	 * 
	 * @param propagant
	 */
	public void unregisterListener(JLFUListener propagant) {
		synchronized (propagateTo) {
			propagateTo.remove(propagant);
		}
	}


	/**
	 * Unregister all the listeners.
	 */
	public void unregisterAllListeners() {
		synchronized (propagateTo) {
			propagateTo.clear();
		}
	}


	/**
	 * @return the propagator.
	 */
	public T getPropagator() {
		return proxiedElement;
	}
}
