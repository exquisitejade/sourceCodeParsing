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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link DefaultOAuth2AuthorizationRequestResolver}.
 *
 * @author Joe Grandja
 */
public class DefaultOAuth2AuthorizationRequestResolverTests {
	private ClientRegistration registration1;
	private ClientRegistration registration2;
	private ClientRegistrationRepository clientRegistrationRepository;
	private String authorizationRequestBaseUri = "/oauth2/authorization";
	private DefaultOAuth2AuthorizationRequestResolver resolver;

	@Before
	public void setUp() {
		this.registration1 = TestClientRegistrations.clientRegistration().build();
		this.registration2 = TestClientRegistrations.clientRegistration2().build();
		this.clientRegistrationRepository = new InMemoryClientRegistrationRepository(
				this.registration1, this.registration2);
		this.resolver = new DefaultOAuth2AuthorizationRequestResolver(
				this.clientRegistrationRepository, this.authorizationRequestBaseUri);
	}

	@Test
	public void constructorWhenClientRegistrationRepositoryIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new DefaultOAuth2AuthorizationRequestResolver(null, this.authorizationRequestBaseUri))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void constructorWhenAuthorizationRequestBaseUriIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new DefaultOAuth2AuthorizationRequestResolver(this.clientRegistrationRepository, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void resolveWhenNotAuthorizationRequestThenDoesNotResolve() {
		String requestUri = "/path";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);

		OAuth2AuthorizationRequest authorizationRequest = this.resolver.resolve(request);
		assertThat(authorizationRequest).isNull();
	}

	@Test
	public void resolveWhenAuthorizationRequestWithInvalidClientThenThrowIllegalArgumentException() {
		ClientRegistration clientRegistration = this.registration1;
		String requestUri = this.authorizationRequestBaseUri + "/" + clientRegistration.getRegistrationId() + "-invalid";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);

		assertThatThrownBy(() -> this.resolver.resolve(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Invalid Client Registration with Id: " + clientRegistration.getRegistrationId() + "-invalid");
	}

	@Test
	public void resolveWhenAuthorizationRequestWithValidClientThenResolves() {
		ClientRegistration clientRegistration = this.registration1;
		String requestUri = this.authorizationRequestBaseUri + "/" + clientRegistration.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);

		OAuth2AuthorizationRequest authorizationRequest = this.resolver.resolve(request);
		assertThat(authorizationRequest).isNotNull();
		assertThat(authorizationRequest.getAuthorizationUri()).isEqualTo(
				clientRegistration.getProviderDetails().getAuthorizationUri());
		assertThat(authorizationRequest.getGrantType()).isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(authorizationRequest.getResponseType()).isEqualTo(OAuth2AuthorizationResponseType.CODE);
		assertThat(authorizationRequest.getClientId()).isEqualTo(clientRegistration.getClientId());
		assertThat(authorizationRequest.getRedirectUri())
				.isEqualTo("http://localhost/login/oauth2/code/" + clientRegistration.getRegistrationId());
		assertThat(authorizationRequest.getScopes()).isEqualTo(clientRegistration.getScopes());
		assertThat(authorizationRequest.getState()).isNotNull();
		assertThat(authorizationRequest.getAdditionalParameters())
				.containsExactly(entry(OAuth2ParameterNames.REGISTRATION_ID, clientRegistration.getRegistrationId()));
		assertThat(authorizationRequest.getAuthorizationRequestUri()).matches("https://example.com/login/oauth/authorize\\?response_type=code&client_id=client-id&scope=read%3Auser&state=.{15,}&redirect_uri=http%3A%2F%2Flocalhost%2Flogin%2Foauth2%2Fcode%2Fregistration-id");
	}

	@Test
	public void resolveWhenClientAuthorizationRequiredExceptionAvailableThenResolves() {
		ClientRegistration clientRegistration = this.registration2;
		String requestUri = "/path";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);

		OAuth2AuthorizationRequest authorizationRequest = this.resolver.resolve(request, clientRegistration.getRegistrationId());
		assertThat(authorizationRequest).isNotNull();
		assertThat(authorizationRequest.getAdditionalParameters())
				.containsExactly(entry(OAuth2ParameterNames.REGISTRATION_ID, clientRegistration.getRegistrationId()));
	}

	@Test
	public void resolveWhenAuthorizationRequestRedirectUriTemplatedThenRedirectUriExpanded() {
		ClientRegistration clientRegistration = this.registration2;
		String requestUri = this.authorizationRequestBaseUri + "/" + clientRegistration.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);

		OAuth2AuthorizationRequest authorizationRequest = this.resolver.resolve(request);
		assertThat(authorizationRequest.getRedirectUri()).isNotEqualTo(
				clientRegistration.getRedirectUriTemplate());
		assertThat(authorizationRequest.getRedirectUri()).isEqualTo(
				"http://localhost/login/oauth2/code/" + clientRegistration.getRegistrationId());
	}

	// gh-5520
	@Test
	public void resolveWhenAuthorizationRequestRedirectUriTemplatedThenRedirectUriExpandedExcludesQueryString() {
		ClientRegistration clientRegistration = this.registration2;
		String requestUri = this.authorizationRequestBaseUri + "/" + clientRegistration.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		request.setQueryString("foo=bar");

		OAuth2AuthorizationRequest authorizationRequest = this.resolver.resolve(request);
		assertThat(authorizationRequest.getRedirectUri()).isNotEqualTo(
				clientRegistration.getRedirectUriTemplate());
		assertThat(authorizationRequest.getRedirectUri()).isEqualTo(
				"http://localhost/login/oauth2/code/" + clientRegistration.getRegistrationId());
	}

	@Test
	public void resolveWhenAuthorizationRequestIncludesPort80ThenExpandedRedirectUriExcludesPort() {
		ClientRegistration clientRegistration = this.registration1;
		String requestUri = this.authorizationRequestBaseUri + "/" + clientRegistration.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setScheme("http");
		request.setServerName("example.com");
		request.setServerPort(80);
		request.setServletPath(requestUri);

		OAuth2AuthorizationRequest authorizationRequest = this.resolver.resolve(request);
		assertThat(authorizationRequest.getAuthorizationRequestUri()).matches("https://example.com/login/oauth/authorize\\?response_type=code&client_id=client-id&scope=read%3Auser&state=.{15,}&redirect_uri=http%3A%2F%2Fexample.com%2Flogin%2Foauth2%2Fcode%2Fregistration-id");
	}

	@Test
	public void resolveWhenAuthorizationRequestIncludesPort443ThenExpandedRedirectUriExcludesPort() {
		ClientRegistration clientRegistration = this.registration1;
		String requestUri = this.authorizationRequestBaseUri + "/" + clientRegistration.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setScheme("https");
		request.setServerName("example.com");
		request.setServerPort(443);
		request.setServletPath(requestUri);

		OAuth2AuthorizationRequest authorizationRequest = this.resolver.resolve(request);
		assertThat(authorizationRequest.getAuthorizationRequestUri()).matches("https://example.com/login/oauth/authorize\\?response_type=code&client_id=client-id&scope=read%3Auser&state=.{15,}&redirect_uri=https%3A%2F%2Fexample.com%2Flogin%2Foauth2%2Fcode%2Fregistration-id");
	}

	@Test
	public void resolveWhenClientAuthorizationRequiredExceptionAvailableThenRedirectUriIsAuthorize() {
		ClientRegistration clientRegistration = this.registration1;
		String requestUri = "/path";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);

		OAuth2AuthorizationRequest authorizationRequest = this.resolver.resolve(request, clientRegistration.getRegistrationId());
		assertThat(authorizationRequest.getAuthorizationRequestUri()).matches("https://example.com/login/oauth/authorize\\?response_type=code&client_id=client-id&scope=read%3Auser&state=.{15,}&redirect_uri=http%3A%2F%2Flocalhost%2Fauthorize%2Foauth2%2Fcode%2Fregistration-id");
	}

	@Test
	public void resolveWhenAuthorizationRequestOAuth2LoginThenRedirectUriIsLogin() {
		ClientRegistration clientRegistration = this.registration2;
		String requestUri = this.authorizationRequestBaseUri + "/" + clientRegistration.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);

		OAuth2AuthorizationRequest authorizationRequest = this.resolver.resolve(request);
		assertThat(authorizationRequest.getAuthorizationRequestUri()).matches("https://example.com/login/oauth/authorize\\?response_type=code&client_id=client-id-2&scope=read%3Auser&state=.{15,}&redirect_uri=http%3A%2F%2Flocalhost%2Flogin%2Foauth2%2Fcode%2Fregistration-id-2");
	}

	@Test
	public void resolveWhenAuthorizationRequestHasActionParameterAuthorizeThenRedirectUriIsAuthorize() {
		ClientRegistration clientRegistration = this.registration1;
		String requestUri = this.authorizationRequestBaseUri + "/" + clientRegistration.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.addParameter("action", "authorize");
		request.setServletPath(requestUri);

		OAuth2AuthorizationRequest authorizationRequest = this.resolver.resolve(request);
		assertThat(authorizationRequest.getAuthorizationRequestUri()).matches("https://example.com/login/oauth/authorize\\?response_type=code&client_id=client-id&scope=read%3Auser&state=.{15,}&redirect_uri=http%3A%2F%2Flocalhost%2Fauthorize%2Foauth2%2Fcode%2Fregistration-id");
	}

	@Test
	public void resolveWhenAuthorizationRequestHasActionParameterLoginThenRedirectUriIsLogin() {
		ClientRegistration clientRegistration = this.registration2;
		String requestUri = this.authorizationRequestBaseUri + "/" + clientRegistration.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.addParameter("action", "login");
		request.setServletPath(requestUri);

		OAuth2AuthorizationRequest authorizationRequest = this.resolver.resolve(request);
		assertThat(authorizationRequest.getAuthorizationRequestUri()).matches("https://example.com/login/oauth/authorize\\?response_type=code&client_id=client-id-2&scope=read%3Auser&state=.{15,}&redirect_uri=http%3A%2F%2Flocalhost%2Flogin%2Foauth2%2Fcode%2Fregistration-id-2");
	}
}
