/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.web.context;

import static org.assertj.core.api.Assertions.assertThat;

import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 *
 * @author Rob Winch
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class SaveContextOnUpdateOrErrorResponseWrapperTests {
	@Mock
	private SecurityContext securityContext;

	private MockHttpServletResponse response;
	private SaveContextOnUpdateOrErrorResponseWrapperStub wrappedResponse;

	@Before
	public void setUp() {
		response = new MockHttpServletResponse();
		wrappedResponse = new SaveContextOnUpdateOrErrorResponseWrapperStub(response,
				true);
		SecurityContextHolder.setContext(securityContext);
	}

	@After
	public void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void sendErrorSavesSecurityContext() throws Exception {
		int error = HttpServletResponse.SC_FORBIDDEN;
		wrappedResponse.sendError(error);
		assertThat(wrappedResponse.securityContext).isEqualTo(securityContext);
		assertThat(response.getStatus()).isEqualTo(error);
	}

	@Test
	public void sendErrorSkipsSaveSecurityContextDisables() throws Exception {
		final int error = HttpServletResponse.SC_FORBIDDEN;
		wrappedResponse.disableSaveOnResponseCommitted();
		wrappedResponse.sendError(error);
		assertThat(wrappedResponse.securityContext).isNull();
		assertThat(response.getStatus()).isEqualTo(error);
	}

	@Test
	public void sendErrorWithMessageSavesSecurityContext() throws Exception {
		int error = HttpServletResponse.SC_FORBIDDEN;
		String message = "Forbidden";
		wrappedResponse.sendError(error, message);
		assertThat(wrappedResponse.securityContext).isEqualTo(securityContext);
		assertThat(response.getStatus()).isEqualTo(error);
		assertThat(response.getErrorMessage()).isEqualTo(message);
	}

	@Test
	public void sendErrorWithMessageSkipsSaveSecurityContextDisables() throws Exception {
		final int error = HttpServletResponse.SC_FORBIDDEN;
		final String message = "Forbidden";
		wrappedResponse.disableSaveOnResponseCommitted();
		wrappedResponse.sendError(error, message);
		assertThat(wrappedResponse.securityContext).isNull();
		assertThat(response.getStatus()).isEqualTo(error);
		assertThat(response.getErrorMessage()).isEqualTo(message);
	}

	@Test
	public void sendRedirectSavesSecurityContext() throws Exception {
		String url = "/location";
		wrappedResponse.sendRedirect(url);
		assertThat(wrappedResponse.securityContext).isEqualTo(securityContext);
		assertThat(response.getRedirectedUrl()).isEqualTo(url);
	}

	@Test
	public void sendRedirectSkipsSaveSecurityContextDisables() throws Exception {
		final String url = "/location";
		wrappedResponse.disableSaveOnResponseCommitted();
		wrappedResponse.sendRedirect(url);
		assertThat(wrappedResponse.securityContext).isNull();
		assertThat(response.getRedirectedUrl()).isEqualTo(url);
	}

	@Test
	public void outputFlushSavesSecurityContext() throws Exception {
		wrappedResponse.getOutputStream().flush();
		assertThat(wrappedResponse.securityContext).isEqualTo(securityContext);
	}

	@Test
	public void outputFlushSkipsSaveSecurityContextDisables() throws Exception {
		wrappedResponse.disableSaveOnResponseCommitted();
		wrappedResponse.getOutputStream().flush();
		assertThat(wrappedResponse.securityContext).isNull();
	}

	@Test
	public void outputCloseSavesSecurityContext() throws Exception {
		wrappedResponse.getOutputStream().close();
		assertThat(wrappedResponse.securityContext).isEqualTo(securityContext);
	}

	@Test
	public void outputCloseSkipsSaveSecurityContextDisables() throws Exception {
		wrappedResponse.disableSaveOnResponseCommitted();
		wrappedResponse.getOutputStream().close();
		assertThat(wrappedResponse.securityContext).isNull();
	}

	@Test
	public void writerFlushSavesSecurityContext() throws Exception {
		wrappedResponse.getWriter().flush();
		assertThat(wrappedResponse.securityContext).isEqualTo(securityContext);
	}

	@Test
	public void writerFlushSkipsSaveSecurityContextDisables() throws Exception {
		wrappedResponse.disableSaveOnResponseCommitted();
		wrappedResponse.getWriter().flush();
		assertThat(wrappedResponse.securityContext).isNull();
	}

	@Test
	public void writerCloseSavesSecurityContext() throws Exception {
		wrappedResponse.getWriter().close();
		assertThat(wrappedResponse.securityContext).isEqualTo(securityContext);
	}

	@Test
	public void writerCloseSkipsSaveSecurityContextDisables() throws Exception {
		wrappedResponse.disableSaveOnResponseCommitted();
		wrappedResponse.getWriter().close();
		assertThat(wrappedResponse.securityContext).isNull();
	}

	@Test
	public void flushBufferSavesSecurityContext() throws Exception {
		wrappedResponse.flushBuffer();
		assertThat(wrappedResponse.securityContext).isEqualTo(securityContext);
	}

	@Test
	public void flushBufferSkipsSaveSecurityContextDisables() throws Exception {
		wrappedResponse.disableSaveOnResponseCommitted();
		wrappedResponse.flushBuffer();
		assertThat(wrappedResponse.securityContext).isNull();
	}

	private static class SaveContextOnUpdateOrErrorResponseWrapperStub extends
			SaveContextOnUpdateOrErrorResponseWrapper {
		private SecurityContext securityContext;

		public SaveContextOnUpdateOrErrorResponseWrapperStub(
				HttpServletResponse response, boolean disableUrlRewriting) {
			super(response, disableUrlRewriting);
		}

		@Override
		protected void saveContext(SecurityContext context) {
			securityContext = context;
		}
	}
}
