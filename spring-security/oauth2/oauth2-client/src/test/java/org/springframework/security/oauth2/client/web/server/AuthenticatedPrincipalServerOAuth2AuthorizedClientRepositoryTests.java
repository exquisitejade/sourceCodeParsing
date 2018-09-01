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
package org.springframework.security.oauth2.client.web.server;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Rob Winch
 */
public class AuthenticatedPrincipalServerOAuth2AuthorizedClientRepositoryTests {
	private String registrationId = "registrationId";
	private String principalName = "principalName";
	private ReactiveOAuth2AuthorizedClientService authorizedClientService;
	private ServerOAuth2AuthorizedClientRepository anonymousAuthorizedClientRepository;
	private AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository authorizedClientRepository;

	private MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

	@Before
	public void setup() {
		this.authorizedClientService = mock(ReactiveOAuth2AuthorizedClientService.class);
		this.anonymousAuthorizedClientRepository = mock(
				ServerOAuth2AuthorizedClientRepository.class);
		this.authorizedClientRepository = new AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(this.authorizedClientService);
		this.authorizedClientRepository.setAnonymousAuthorizedClientRepository(this.anonymousAuthorizedClientRepository);
	}

	@Test
	public void constructorWhenAuthorizedClientServiceIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void setAuthorizedClientRepositoryWhenAuthorizedClientRepositoryIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.authorizedClientRepository.setAnonymousAuthorizedClientRepository(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void loadAuthorizedClientWhenAuthenticatedPrincipalThenLoadFromService() {
		when(this.authorizedClientService.loadAuthorizedClient(any(), any())).thenReturn(Mono.empty());
		Authentication authentication = this.createAuthenticatedPrincipal();
		this.authorizedClientRepository.loadAuthorizedClient(this.registrationId, authentication, this.exchange).block();
		verify(this.authorizedClientService).loadAuthorizedClient(this.registrationId, this.principalName);
	}

	@Test
	public void loadAuthorizedClientWhenAnonymousPrincipalThenLoadFromAnonymousRepository() {
		when(this.anonymousAuthorizedClientRepository.loadAuthorizedClient(any(), any(), any())).thenReturn(Mono.empty());
		Authentication authentication = this.createAnonymousPrincipal();
		this.authorizedClientRepository.loadAuthorizedClient(this.registrationId, authentication, this.exchange).block();
		verify(this.anonymousAuthorizedClientRepository).loadAuthorizedClient(this.registrationId, authentication, this.exchange);
	}

	@Test
	public void saveAuthorizedClientWhenAuthenticatedPrincipalThenSaveToService() {
		when(this.authorizedClientService.saveAuthorizedClient(any(), any())).thenReturn(Mono.empty());
		Authentication authentication = this.createAuthenticatedPrincipal();
		OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
		this.authorizedClientRepository.saveAuthorizedClient(authorizedClient, authentication, this.exchange).block();
		verify(this.authorizedClientService).saveAuthorizedClient(authorizedClient, authentication);
	}

	@Test
	public void saveAuthorizedClientWhenAnonymousPrincipalThenSaveToAnonymousRepository() {
		when(this.anonymousAuthorizedClientRepository.saveAuthorizedClient(any(), any(), any())).thenReturn(Mono.empty());
		Authentication authentication = this.createAnonymousPrincipal();
		OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
		this.authorizedClientRepository.saveAuthorizedClient(authorizedClient, authentication, this.exchange).block();
		verify(this.anonymousAuthorizedClientRepository).saveAuthorizedClient(authorizedClient, authentication, this.exchange);
	}

	@Test
	public void removeAuthorizedClientWhenAuthenticatedPrincipalThenRemoveFromService() {
		when(this.authorizedClientService.removeAuthorizedClient(any(), any())).thenReturn(Mono.empty());
		Authentication authentication = this.createAuthenticatedPrincipal();
		this.authorizedClientRepository.removeAuthorizedClient(this.registrationId, authentication, this.exchange).block();
		verify(this.authorizedClientService).removeAuthorizedClient(this.registrationId, this.principalName);
	}

	@Test
	public void removeAuthorizedClientWhenAnonymousPrincipalThenRemoveFromAnonymousRepository() {
		when(this.anonymousAuthorizedClientRepository.removeAuthorizedClient(any(), any(), any())).thenReturn(Mono.empty());
		Authentication authentication = this.createAnonymousPrincipal();
		this.authorizedClientRepository.removeAuthorizedClient(this.registrationId, authentication, this.exchange).block();
		verify(this.anonymousAuthorizedClientRepository).removeAuthorizedClient(this.registrationId, authentication, this.exchange);
	}

	private Authentication createAuthenticatedPrincipal() {
		TestingAuthenticationToken authentication = new TestingAuthenticationToken(this.principalName, "password");
		authentication.setAuthenticated(true);
		return authentication;
	}

	private Authentication createAnonymousPrincipal() {
		return new AnonymousAuthenticationToken("key-1234", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
	}
}
