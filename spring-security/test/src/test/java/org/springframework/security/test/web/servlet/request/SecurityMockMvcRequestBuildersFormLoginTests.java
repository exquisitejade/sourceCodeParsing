/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.security.test.web.servlet.request;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.CsrfRequestPostProcessor;
import org.springframework.security.web.csrf.CsrfToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;

public class SecurityMockMvcRequestBuildersFormLoginTests {
	private MockServletContext servletContext;

	@Before
	public void setup() throws Exception {
		this.servletContext = new MockServletContext();
	}

	@Test
	public void defaults() throws Exception {
		MockHttpServletRequest request = formLogin().buildRequest(this.servletContext);
		CsrfToken token = (CsrfToken) request
				.getAttribute(CsrfRequestPostProcessor.TestCsrfTokenRepository.TOKEN_ATTR_NAME);

		assertThat(request.getParameter("username")).isEqualTo("user");
		assertThat(request.getParameter("password")).isEqualTo("password");
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getParameter(token.getParameterName()))
				.isEqualTo(token.getToken());
		assertThat(request.getRequestURI()).isEqualTo("/login");
		assertThat(request.getParameter("_csrf")).isNotNull();
	}

	@Test
	public void custom() throws Exception {
		MockHttpServletRequest request = formLogin("/login").user("username", "admin")
				.password("password", "secret").buildRequest(this.servletContext);

		CsrfToken token = (CsrfToken) request
				.getAttribute(CsrfRequestPostProcessor.TestCsrfTokenRepository.TOKEN_ATTR_NAME);

		assertThat(request.getParameter("username")).isEqualTo("admin");
		assertThat(request.getParameter("password")).isEqualTo("secret");
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getParameter(token.getParameterName()))
				.isEqualTo(token.getToken());
		assertThat(request.getRequestURI()).isEqualTo("/login");
	}

	// gh-3920
	@Test
	public void usesAcceptMediaForContentNegotiation() throws Exception {
		MockHttpServletRequest request = formLogin("/login").user("username", "admin")
				.password("password", "secret").buildRequest(this.servletContext);

		assertThat(request.getHeader("Accept"))
				.isEqualTo(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
	}
}
