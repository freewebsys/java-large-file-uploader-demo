package com.am.jlfu.authorizer;


import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.fileuploader.exception.AuthorizationException;
import com.am.jlfu.fileuploader.web.UploadServletAction;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class DefaultAuthorizerTest {

	@Autowired
	Authorizer authorizer;



	@Test
	public void test()
			throws AuthorizationException {
		authorizer.getAuthorization(null, null, null, null);
	}


	@Test(expected = AuthorizationException.class)
	public void testException()
			throws AuthorizationException {
		new Authorizer() {

			@Override
			public void getAuthorization(HttpServletRequest request, UploadServletAction action, UUID clientId, UUID... optionalFileId)
					throws AuthorizationException {
				throw new AuthorizationException(action, clientId, optionalFileId);
			}
		}.getAuthorization(null, null, null, null);
	}
}
