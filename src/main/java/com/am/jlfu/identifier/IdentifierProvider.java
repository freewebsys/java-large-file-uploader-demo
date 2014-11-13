package com.am.jlfu.identifier;


import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.am.jlfu.identifier.impl.DefaultIdentifierProvider;



/**
 * Provides identification for JLFU API.<br>
 * The default implementation ({@link DefaultIdentifierProvider}) is using a cookie to store a UUID
 * generated for every client.<br>
 * You can provide your own implementation to be able to manage this identification using a more
 * complex system (if you want users to resume files from another browser or provide ids linked to a
 * job instead of a client if you are providing upload in a more sequential way).
 * 
 * @author antoinem
 * @see DefaultIdentifierProvider
 * 
 */
public interface IdentifierProvider {

	/**
	 * Retrieves the client identifier.<br>
	 * 
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * 
	 * @return the unique identifier identifying this client/job
	 */
	UUID getIdentifier(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);


	/**
	 * Define this method if your application supports IDs specified by the client (on
	 * initialization).
	 * 
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @param id
	 * 
	 */
	void setIdentifier(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, UUID id);


	/**
	 * Removes the identifier.
	 * 
	 * @param request
	 * @param response
	 */
	void clearIdentifier(HttpServletRequest request, HttpServletResponse response);

}
