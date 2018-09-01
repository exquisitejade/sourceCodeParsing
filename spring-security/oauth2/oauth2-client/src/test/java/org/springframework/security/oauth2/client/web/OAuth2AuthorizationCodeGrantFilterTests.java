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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link OAuth2AuthorizationCodeGrantFilter}.
 *
 * @author Joe Grandja
 */
@PowerMockIgnore("javax.security.*")
@PrepareForTest({OAuth2AuthorizationRequest.class, OAuth2AuthorizationExchange.class, OAuth2AuthorizationCodeGrantFilter.class})
@RunWith(PowerMockRunner.class)
public class OAuth2AuthorizationCodeGrantFilterTests {
	private ClientRegistration registration1;
	private String principalName1 = "principal-1";
	private ClientRegistrationRepository clientRegistrationRepository;
	private OAuth2AuthorizedClientService authorizedClientService;
	private OAuth2AuthorizedClientRepository authorizedClientRepository;
	private AuthenticationManager authenticationManager;
	private AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;
	private OAuth2AuthorizationCodeGrantFilter filter;

	@Before
	public void setup() {
		this.registration1 = TestClientRegistrations.clientRegistration().build();
		this.clientRegistrationRepository = new InMemoryClientRegistrationRepository(this.registration1);
		this.authorizedClientService = new InMemoryOAuth2AuthorizedClientService(this.clientRegistrationRepository);
		this.authorizedClientRepository = new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(this.authorizedClientService);
		this.authorizationRequestRepository = new HttpSessionOAuth2AuthorizationRequestRepository();
		this.authenticationManager = mock(AuthenticationManager.class);
		this.filter = spy(new OAuth2AuthorizationCodeGrantFilter(
			this.clientRegistrationRepository, this.authorizedClientRepository, this.authenticationManager));
		this.filter.setAuthorizationRequestRepository(this.authorizationRequestRepository);
		TestingAuthenticationToken authentication = new TestingAuthenticationToken(this.principalName1, "password");
		authentication.setAuthenticated(true);
		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		securityContext.setAuthentication(authentication);
		SecurityContextHolder.setContext(securityContext);
	}

	@After
	public void cleanup() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void constructorWhenClientRegistrationRepositoryIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new OAuth2AuthorizationCodeGrantFilter(null, this.authorizedClientRepository, this.authenticationManager))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void constructorWhenAuthorizedClientRepositoryIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new OAuth2AuthorizationCodeGrantFilter(this.clientRegistrationRepository, null, this.authenticationManager))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void constructorWhenAuthenticationManagerIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new OAuth2AuthorizationCodeGrantFilter(this.clientRegistrationRepository, this.authorizedClientRepository, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void setAuthorizationRequestRepositoryWhenAuthorizationRequestRepositoryIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.filter.setAuthorizationRequestRepository(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void doFilterWhenNotAuthorizationResponseThenNotProcessed() throws Exception {
		String requestUri = "/path";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		// NOTE: A valid Authorization Response contains either a 'code' or 'error' parameter.

		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain filterChain = mock(FilterChain.class);

		this.filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
	}

	@Test
	public void doFilterWhenAuthorizationRequestNotFoundThenNotProcessed() throws Exception {
		String requestUri = "/path";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		request.addParameter(OAuth2ParameterNames.CODE, "code");
		request.addParameter(OAuth2ParameterNames.STATE, "state");

		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain filterChain = mock(FilterChain.class);

		this.filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
	}

	@Test
	public void doFilterWhenAuthorizationResponseUrlDoesNotMatchAuthorizationRequestRedirectUriThenNotProcessed() throws Exception {
		String requestUri = "/callback/client-1";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		request.addParameter(OAuth2ParameterNames.CODE, "code");
		request.addParameter(OAuth2ParameterNames.STATE, "state");

		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain filterChain = mock(FilterChain.class);

		this.setUpAuthorizationRequest(request, response, this.registration1);
		request.setRequestURI(requestUri + "-no-match");

		this.filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
	}

	@Test
	public void doFilterWhenAuthorizationResponseValidThenAuthorizationRequestRemoved() throws Exception {
		String requestUri = "/callback/client-1";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		request.addParameter(OAuth2ParameterNames.CODE, "code");
		request.addParameter(OAuth2ParameterNames.STATE, "state");

		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		this.setUpAuthorizationRequest(request, response, this.registration1);
		this.setUpAuthenticationResult(this.registration1);

		this.filter.doFilter(request, response, filterChain);

		assertThat(this.authorizationRequestRepository.loadAuthorizationRequest(request)).isNull();
	}

	@Test
	public void doFilterWhenAuthenticationFailsThenHandleOAuth2AuthenticationException() throws Exception {
		String requestUri = "/callback/client-1";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		request.addParameter(OAuth2ParameterNames.CODE, "code");
		request.addParameter(OAuth2ParameterNames.STATE, "state");

		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		this.setUpAuthorizationRequest(request, response, this.registration1);
		OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT);
		when(this.authenticationManager.authenticate(any(Authentication.class)))
			.thenThrow(new OAuth2AuthenticationException(error, error.toString()));

		this.filter.doFilter(request, response, filterChain);

		assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost/callback/client-1?error=invalid_grant");
	}

	@Test
	public void doFilterWhenAuthorizationResponseSuccessThenAuthorizedClientSavedToService() throws Exception {
		String requestUri = "/callback/client-1";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		request.addParameter(OAuth2ParameterNames.CODE, "code");
		request.addParameter(OAuth2ParameterNames.STATE, "state");

		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		this.setUpAuthorizationRequest(request, response, this.registration1);
		this.setUpAuthenticationResult(this.registration1);

		this.filter.doFilter(request, response, filterChain);

		OAuth2AuthorizedClient authorizedClient = this.authorizedClientService.loadAuthorizedClient(
			this.registration1.getRegistrationId(), this.principalName1);
		assertThat(authorizedClient).isNotNull();
		assertThat(authorizedClient.getClientRegistration()).isEqualTo(this.registration1);
		assertThat(authorizedClient.getPrincipalName()).isEqualTo(this.principalName1);
		assertThat(authorizedClient.getAccessToken()).isNotNull();
		assertThat(authorizedClient.getRefreshToken()).isNotNull();
	}

	@Test
	public void doFilterWhenAuthorizationResponseSuccessThenRedirected() throws Exception {
		String requestUri = "/callback/client-1";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		request.addParameter(OAuth2ParameterNames.CODE, "code");
		request.addParameter(OAuth2ParameterNames.STATE, "state");

		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		this.setUpAuthorizationRequest(request, response, this.registration1);
		this.setUpAuthenticationResult(this.registration1);

		this.filter.doFilter(request, response, filterChain);

		assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost/callback/client-1");
	}

	@Test
	public void doFilterWhenAuthorizationResponseSuccessHasSavedRequestThenRedirectedToSavedRequest() throws Exception {
		String requestUri = "/saved-request";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		RequestCache requestCache = new HttpSessionRequestCache();
		requestCache.saveRequest(request, response);

		requestUri = "/callback/client-1";
		request.setRequestURI(requestUri);
		request.addParameter(OAuth2ParameterNames.CODE, "code");
		request.addParameter(OAuth2ParameterNames.STATE, "state");

		FilterChain filterChain = mock(FilterChain.class);

		this.setUpAuthorizationRequest(request, response, this.registration1);
		this.setUpAuthenticationResult(this.registration1);

		this.filter.doFilter(request, response, filterChain);

		assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost/saved-request");
	}

	@Test
	public void doFilterWhenAuthorizationResponseSuccessAndAnonymousAccessThenAuthorizedClientSavedToHttpSession() throws Exception {
		AnonymousAuthenticationToken anonymousPrincipal =
				new AnonymousAuthenticationToken("key-1234", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		securityContext.setAuthentication(anonymousPrincipal);
		SecurityContextHolder.setContext(securityContext);

		String requestUri = "/callback/client-1";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		request.addParameter(OAuth2ParameterNames.CODE, "code");
		request.addParameter(OAuth2ParameterNames.STATE, "state");

		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		this.setUpAuthorizationRequest(request, response, this.registration1);
		this.setUpAuthenticationResult(this.registration1);

		this.filter.doFilter(request, response, filterChain);

		OAuth2AuthorizedClient authorizedClient = this.authorizedClientRepository.loadAuthorizedClient(
				this.registration1.getRegistrationId(), anonymousPrincipal, request);
		assertThat(authorizedClient).isNotNull();

		assertThat(authorizedClient.getClientRegistration()).isEqualTo(this.registration1);
		assertThat(authorizedClient.getPrincipalName()).isEqualTo(anonymousPrincipal.getName());
		assertThat(authorizedClient.getAccessToken()).isNotNull();

		HttpSession session = request.getSession(false);
		assertThat(session).isNotNull();

		@SuppressWarnings("unchecked")
		Map<String, OAuth2AuthorizedClient> authorizedClients = (Map<String, OAuth2AuthorizedClient>)
				session.getAttribute(HttpSessionOAuth2AuthorizedClientRepository.class.getName() + ".AUTHORIZED_CLIENTS");
		assertThat(authorizedClients).isNotEmpty();
		assertThat(authorizedClients).hasSize(1);
		assertThat(authorizedClients.values().iterator().next()).isSameAs(authorizedClient);
	}

	@Test
	public void doFilterWhenAuthorizationResponseSuccessAndAnonymousAccessNullAuthenticationThenAuthorizedClientSavedToHttpSession() throws Exception {
		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		SecurityContextHolder.setContext(securityContext);		// null Authentication

		String requestUri = "/callback/client-1";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		request.addParameter(OAuth2ParameterNames.CODE, "code");
		request.addParameter(OAuth2ParameterNames.STATE, "state");

		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		this.setUpAuthorizationRequest(request, response, this.registration1);
		this.setUpAuthenticationResult(this.registration1);

		this.filter.doFilter(request, response, filterChain);

		OAuth2AuthorizedClient authorizedClient = this.authorizedClientRepository.loadAuthorizedClient(
				this.registration1.getRegistrationId(), null, request);
		assertThat(authorizedClient).isNotNull();

		assertThat(authorizedClient.getClientRegistration()).isEqualTo(this.registration1);
		assertThat(authorizedClient.getPrincipalName()).isEqualTo("anonymousUser");
		assertThat(authorizedClient.getAccessToken()).isNotNull();

		HttpSession session = request.getSession(false);
		assertThat(session).isNotNull();

		@SuppressWarnings("unchecked")
		Map<String, OAuth2AuthorizedClient> authorizedClients = (Map<String, OAuth2AuthorizedClient>)
				session.getAttribute(HttpSessionOAuth2AuthorizedClientRepository.class.getName() + ".AUTHORIZED_CLIENTS");
		assertThat(authorizedClients).isNotEmpty();
		assertThat(authorizedClients).hasSize(1);
		assertThat(authorizedClients.values().iterator().next()).isSameAs(authorizedClient);
	}

	private void setUpAuthorizationRequest(HttpServletRequest request, HttpServletResponse response,
											ClientRegistration registration) {
		Map<String, Object> additionalParameters = new HashMap<>();
		additionalParameters.put(OAuth2ParameterNames.REGISTRATION_ID, registration.getRegistrationId());
		OAuth2AuthorizationRequest authorizationRequest = mock(OAuth2AuthorizationRequest.class);
		when(authorizationRequest.getAdditionalParameters()).thenReturn(additionalParameters);
		when(authorizationRequest.getRedirectUri()).thenReturn(request.getRequestURL().toString());
		when(authorizationRequest.getState()).thenReturn("state");
		this.authorizationRequestRepository.saveAuthorizationRequest(authorizationRequest, request, response);
	}

	private void setUpAuthenticationResult(ClientRegistration registration) {
		OAuth2AuthorizationCodeAuthenticationToken authentication = mock(OAuth2AuthorizationCodeAuthenticationToken.class);
		when(authentication.getClientRegistration()).thenReturn(registration);
		when(authentication.getAuthorizationExchange()).thenReturn(mock(OAuth2AuthorizationExchange.class));
		when(authentication.getAccessToken()).thenReturn(mock(OAuth2AccessToken.class));
		when(authentication.getRefreshToken()).thenReturn(mock(OAuth2RefreshToken.class));
		when(this.authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
	}
}
