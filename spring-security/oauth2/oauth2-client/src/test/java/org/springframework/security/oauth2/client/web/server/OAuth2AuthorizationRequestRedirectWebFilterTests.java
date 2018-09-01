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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.handler.FilteringWebHandler;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Rob Winch
 * @since 5.1
 */
@RunWith(MockitoJUnitRunner.class)
public class OAuth2AuthorizationRequestRedirectWebFilterTests {
	@Mock
	private ReactiveClientRegistrationRepository clientRepository;

	@Mock
	private ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authzRequestRepository;

	private ClientRegistration registration = TestClientRegistrations.clientRegistration().build();

	private OAuth2AuthorizationRequestRedirectWebFilter filter;

	private WebTestClient client;

	@Before
	public void setup() {
		this.filter = new OAuth2AuthorizationRequestRedirectWebFilter(this.clientRepository);
		this.filter.setAuthorizationRequestRepository(this.authzRequestRepository);
		FilteringWebHandler webHandler = new FilteringWebHandler(e -> e.getResponse().setComplete(), Arrays.asList(this.filter));

		this.client = WebTestClient.bindToWebHandler(webHandler).build();
		when(this.clientRepository.findByRegistrationId(this.registration.getRegistrationId())).thenReturn(
				Mono.just(this.registration));
		when(this.authzRequestRepository.saveAuthorizationRequest(any(), any())).thenReturn(
				Mono.empty());
	}

	@Test
	public void constructorWhenClientRegistrationRepositoryNullThenIllegalArgumentException() {
		this.clientRepository = null;
		assertThatThrownBy(() -> new OAuth2AuthorizationRequestRedirectWebFilter(this.clientRepository))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void filterWhenDoesNotMatchThenClientRegistrationRepositoryNotSubscribed() {
		this.client.get()
				.exchange()
				.expectStatus().isOk();

		verifyZeroInteractions(this.clientRepository, this.authzRequestRepository);
	}

	@Test
	public void filterWhenDoesMatchThenClientRegistrationRepositoryNotSubscribed() {
		FluxExchangeResult<String> result = this.client.get()
				.uri("https://example.com/oauth2/authorization/registration-id").exchange()
				.expectStatus().is3xxRedirection().returnResult(String.class);
		result.assertWithDiagnostics(() -> {
			URI location = result.getResponseHeaders().getLocation();
			assertThat(location)
					.hasScheme("https")
					.hasHost("example.com")
					.hasPath("/login/oauth/authorize")
					.hasParameter("response_type", "code")
					.hasParameter("client_id", "client-id")
					.hasParameter("scope", "read:user")
					.hasParameter("state")
					.hasParameter("redirect_uri", "https://example.com/login/oauth2/code/registration-id");
		});
		verify(this.authzRequestRepository).saveAuthorizationRequest(any(), any());
	}

	// gh-5520
	@Test
	public void filterWhenDoesMatchThenResolveRedirectUriExpandedExcludesQueryString() {
		FluxExchangeResult<String> result = this.client.get()
				.uri("https://example.com/oauth2/authorization/registration-id?foo=bar").exchange()
				.expectStatus().is3xxRedirection().returnResult(String.class);
		result.assertWithDiagnostics(() -> {
			URI location = result.getResponseHeaders().getLocation();
			assertThat(location)
					.hasScheme("https")
					.hasHost("example.com")
					.hasPath("/login/oauth/authorize")
					.hasParameter("response_type", "code")
					.hasParameter("client_id", "client-id")
					.hasParameter("scope", "read:user")
					.hasParameter("state")
					.hasParameter("redirect_uri", "https://example.com/login/oauth2/code/registration-id");
		});
	}

	@Test
	public void filterWhenExceptionThenRedirected() {
		FilteringWebHandler webHandler = new FilteringWebHandler(e -> Mono.error(new ClientAuthorizationRequiredException(this.registration
				.getRegistrationId())), Arrays.asList(this.filter));
		this.client = WebTestClient.bindToWebHandler(webHandler).build();
		FluxExchangeResult<String> result = this.client.get()
				.uri("https://example.com/foo").exchange()
				.expectStatus()
				.is3xxRedirection()
				.returnResult(String.class);
	}
}
