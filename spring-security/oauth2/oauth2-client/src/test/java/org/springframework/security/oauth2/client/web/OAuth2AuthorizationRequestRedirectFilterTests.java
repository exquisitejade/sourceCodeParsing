/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.security.oauth2.client.web;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.util.ClassUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link OAuth2AuthorizationRequestRedirectFilter}.
 *
 * @author Joe Grandja
 */
public class OAuth2AuthorizationRequestRedirectFilterTests {
	private ClientRegistration registration1;
	private ClientRegistration registration2;
	private ClientRegistration registration3;
	private ClientRegistrationRepository clientRegistrationRepository;
	private OAuth2AuthorizationRequestRedirectFilter filter;
	private RequestCache requestCache;

	@Before
	public void setUp() {
		this.registration1 = TestClientRegistrations.clientRegistration().build();
		this.registration2 = TestClientRegistrations.clientRegistration2().build();
		this.registration3 = TestClientRegistrations.clientRegistration()
			.registrationId("registration-3")
			.authorizationGrantType(AuthorizationGrantType.IMPLICIT)
			.redirectUriTemplate("{baseUrl}/authorize/oauth2/implicit/{registrationId}")
			.build();
		this.clientRegistrationRepository = new InMemoryClientRegistrationRepository(
			this.registration1, this.registration2, this.registration3);
		this.filter = new OAuth2AuthorizationRequestRedirectFilter(this.clientRegistrationRepository);
		this.requestCache = mock(RequestCache.class);
		this.filter.setRequestCache(this.requestCache);
	}

	@Test
	public void constructorWhenClientRegistrationRepositoryIsNullThenThrowIllegalArgumentException() {
		Constructor<OAuth2AuthorizationRequestRedirectFilter> constructor = ClassUtils.getConstructorIfAvailable(
				OAuth2AuthorizationRequestRedirectFilter.class, ClientRegistrationRepository.class);
		assertThatThrownBy(() -> constructor.newInstance(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void constructorWhenAuthorizationRequestBaseUriIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new OAuth2AuthorizationRequestRedirectFilter(this.clientRegistrationRepository, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void constructorWhenAuthorizationRequestResolverIsNullThenThrowIllegalArgumentException() {
		Constructor<OAuth2AuthorizationRequestRedirectFilter> constructor = ClassUtils.getConstructorIfAvailable(
				OAuth2AuthorizationRequestRedirectFilter.class, OAuth2AuthorizationRequestResolver.class);
		assertThatThrownBy(() -> constructor.newInstance(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void setAuthorizationRequestRepositoryWhenAuthorizationRequestRepositoryIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.filter.setAuthorizationRequestRepository(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void setRequestCacheWhenRequestCacheIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.filter.setRequestCache(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void doFilterWhenNotAuthorizationRequestThenNextFilter() throws Exception {
		String requestUri = "/path";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		this.filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
	}

	@Test
	public void doFilterWhenAuthorizationRequestWithInvalidClientThenStatusInternalServerError() throws Exception {
		String requestUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI +
			"/" + this.registration1.getRegistrationId() + "-invalid";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		this.filter.doFilter(request, response, filterChain);

		verifyZeroInteractions(filterChain);

		assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
		assertThat(response.getErrorMessage()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
	}

	@Test
	public void doFilterWhenAuthorizationRequestOAuth2LoginThenRedirectForAuthorization() throws Exception {
		String requestUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI +
			"/" + this.registration1.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		this.filter.doFilter(request, response, filterChain);

		verifyZeroInteractions(filterChain);

		assertThat(response.getRedirectedUrl()).matches("https://example.com/login/oauth/authorize\\?response_type=code&client_id=client-id&scope=read%3Auser&state=.{15,}&redirect_uri=http%3A%2F%2Flocalhost%2Flogin%2Foauth2%2Fcode%2Fregistration-id");
	}

	@Test
	public void doFilterWhenAuthorizationRequestOAuth2LoginThenAuthorizationRequestSaved() throws Exception {
		String requestUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI +
			"/" + this.registration2.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository =
				mock(AuthorizationRequestRepository.class);
		this.filter.setAuthorizationRequestRepository(authorizationRequestRepository);

		this.filter.doFilter(request, response, filterChain);

		verifyZeroInteractions(filterChain);
		verify(authorizationRequestRepository).saveAuthorizationRequest(
			any(OAuth2AuthorizationRequest.class), any(HttpServletRequest.class), any(HttpServletResponse.class));
	}

	@Test
	public void doFilterWhenAuthorizationRequestImplicitGrantThenRedirectForAuthorization() throws Exception {
		String requestUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI +
			"/" + this.registration3.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		this.filter.doFilter(request, response, filterChain);

		verifyZeroInteractions(filterChain);

		assertThat(response.getRedirectedUrl()).matches("https://example.com/login/oauth/authorize\\?response_type=token&client_id=client-id&scope=read%3Auser&state=.{15,}&redirect_uri=http%3A%2F%2Flocalhost%2Fauthorize%2Foauth2%2Fimplicit%2Fregistration-3");
	}

	@Test
	public void doFilterWhenAuthorizationRequestImplicitGrantThenAuthorizationRequestNotSaved() throws Exception {
		String requestUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI +
			"/" + this.registration3.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository =
				mock(AuthorizationRequestRepository.class);
		this.filter.setAuthorizationRequestRepository(authorizationRequestRepository);

		this.filter.doFilter(request, response, filterChain);

		verifyZeroInteractions(filterChain);
		verify(authorizationRequestRepository, times(0)).saveAuthorizationRequest(
				any(OAuth2AuthorizationRequest.class), any(HttpServletRequest.class), any(HttpServletResponse.class));
	}

	@Test
	public void doFilterWhenCustomAuthorizationRequestBaseUriThenRedirectForAuthorization() throws Exception {
		String authorizationRequestBaseUri = "/custom/authorization";
		this.filter = new OAuth2AuthorizationRequestRedirectFilter(this.clientRegistrationRepository, authorizationRequestBaseUri);

		String requestUri = authorizationRequestBaseUri + "/" + this.registration1.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		this.filter.doFilter(request, response, filterChain);

		verifyZeroInteractions(filterChain);

		assertThat(response.getRedirectedUrl()).matches("https://example.com/login/oauth/authorize\\?response_type=code&client_id=client-id&scope=read%3Auser&state=.{15,}&redirect_uri=http%3A%2F%2Flocalhost%2Flogin%2Foauth2%2Fcode%2Fregistration-id");
	}

	@Test
	public void doFilterWhenNotAuthorizationRequestAndClientAuthorizationRequiredExceptionThrownThenRedirectForAuthorization() throws Exception {
		String requestUri = "/path";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		doThrow(new ClientAuthorizationRequiredException(this.registration1.getRegistrationId()))
			.when(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

		this.filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

		assertThat(response.getRedirectedUrl()).matches("https://example.com/login/oauth/authorize\\?response_type=code&client_id=client-id&scope=read%3Auser&state=.{15,}&redirect_uri=http%3A%2F%2Flocalhost%2Fauthorize%2Foauth2%2Fcode%2Fregistration-id");
		verify(this.requestCache).saveRequest(any(HttpServletRequest.class), any(HttpServletResponse.class));
	}

	@Test
	public void doFilterWhenNotAuthorizationRequestAndClientAuthorizationRequiredExceptionThrownButAuthorizationRequestNotResolvedThenStatusInternalServerError() throws Exception {
		String requestUri = "/path";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		doThrow(new ClientAuthorizationRequiredException(this.registration1.getRegistrationId()))
				.when(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

		OAuth2AuthorizationRequestResolver resolver = mock(OAuth2AuthorizationRequestResolver.class);
		OAuth2AuthorizationRequestRedirectFilter filter = new OAuth2AuthorizationRequestRedirectFilter(resolver);

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

		verifyZeroInteractions(filterChain);

		assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
		assertThat(response.getErrorMessage()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
	}

	// gh-4911
	@Test
	public void doFilterWhenAuthorizationRequestAndAdditionalParametersProvidedThenAuthorizationRequestIncludesAdditionalParameters() throws Exception {
		String requestUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI +
				"/" + this.registration1.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		request.addParameter("idp", "https://other.provider.com");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		OAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(
				this.clientRegistrationRepository, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);

		OAuth2AuthorizationRequestResolver resolver = mock(OAuth2AuthorizationRequestResolver.class);
		OAuth2AuthorizationRequest result = OAuth2AuthorizationRequest
				.from(defaultAuthorizationRequestResolver.resolve(request))
				.additionalParameters(
						Collections.singletonMap("idp", request.getParameter("idp")))
				.build();
		when(resolver.resolve(any())).thenReturn(result);
		OAuth2AuthorizationRequestRedirectFilter filter = new OAuth2AuthorizationRequestRedirectFilter(resolver);

		filter.doFilter(request, response, filterChain);

		verifyZeroInteractions(filterChain);

		assertThat(response.getRedirectedUrl()).matches("https://example.com/login/oauth/authorize\\?response_type=code&client_id=client-id&scope=read%3Auser&state=.{15,}&redirect_uri=http%3A%2F%2Flocalhost%2Flogin%2Foauth2%2Fcode%2Fregistration-id&idp=https%3A%2F%2Fother.provider.com");
	}

	// gh-4911, gh-5244
	@Test
	public void doFilterWhenAuthorizationRequestAndCustomAuthorizationRequestUriSetThenCustomAuthorizationRequestUriUsed() throws Exception {
		String requestUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI +
				"/" + this.registration1.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		String loginHintParamName = "login_hint";
		request.addParameter(loginHintParamName, "user@provider.com");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		OAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(
				this.clientRegistrationRepository, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);

		OAuth2AuthorizationRequestResolver resolver = mock(OAuth2AuthorizationRequestResolver.class);

		OAuth2AuthorizationRequest defaultAuthorizationRequest = defaultAuthorizationRequestResolver.resolve(request);
		Map<String, Object> additionalParameters = new HashMap<>(defaultAuthorizationRequest.getAdditionalParameters());
		additionalParameters.put(loginHintParamName, request.getParameter(loginHintParamName));
		String customAuthorizationRequestUri = UriComponentsBuilder
				.fromUriString(defaultAuthorizationRequest.getAuthorizationRequestUri())
				.queryParam(loginHintParamName, additionalParameters.get(loginHintParamName))
				.build(true).toUriString();
		OAuth2AuthorizationRequest result = OAuth2AuthorizationRequest
				.from(defaultAuthorizationRequestResolver.resolve(request))
				.additionalParameters(
						Collections.singletonMap("idp", request.getParameter("idp")))
				.authorizationRequestUri(customAuthorizationRequestUri)
				.build();
		when(resolver.resolve(any())).thenReturn(result);

		OAuth2AuthorizationRequestRedirectFilter filter = new OAuth2AuthorizationRequestRedirectFilter(resolver);

		filter.doFilter(request, response, filterChain);

		verifyZeroInteractions(filterChain);

		assertThat(response.getRedirectedUrl()).matches("https://example.com/login/oauth/authorize\\?response_type=code&client_id=client-id&scope=read%3Auser&state=.{15,}&redirect_uri=http%3A%2F%2Flocalhost%2Flogin%2Foauth2%2Fcode%2Fregistration-id&login_hint=user@provider\\.com");
	}
}
