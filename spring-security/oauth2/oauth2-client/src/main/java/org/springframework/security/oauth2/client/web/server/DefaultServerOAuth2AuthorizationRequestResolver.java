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

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * The default implementation of {@link ServerOAuth2AuthorizationRequestResolver}.
 *
 * The {@link ClientRegistration#getRegistrationId()} is extracted from the request using the
 * {@link #DEFAULT_AUTHORIZATION_REQUEST_PATTERN}. The injected {@link ReactiveClientRegistrationRepository} is then
 * used to resolve the {@link ClientRegistration} and create the {@link OAuth2AuthorizationRequest}.
 *
 * @author Rob Winch
 * @since 5.1
 */
public class DefaultServerOAuth2AuthorizationRequestResolver
		implements ServerOAuth2AuthorizationRequestResolver {

	/**
	 * The name of the path variable that contains the {@link ClientRegistration#getRegistrationId()}
	 */
	public static final String DEFAULT_REGISTRATION_ID_URI_VARIABLE_NAME = "registrationId";

	/**
	 * The default pattern used to resolve the {@link ClientRegistration#getRegistrationId()}
	 */
	public static final String DEFAULT_AUTHORIZATION_REQUEST_PATTERN = "/oauth2/authorization/{" + DEFAULT_REGISTRATION_ID_URI_VARIABLE_NAME
			+ "}";

	private final ServerWebExchangeMatcher authorizationRequestMatcher;

	private final ReactiveClientRegistrationRepository clientRegistrationRepository;

	private final StringKeyGenerator stateGenerator = new Base64StringKeyGenerator(Base64.getUrlEncoder());

	/**
	 * Creates a new instance
	 * @param clientRegistrationRepository the repository to resolve the {@link ClientRegistration}
	 */
	public DefaultServerOAuth2AuthorizationRequestResolver(ReactiveClientRegistrationRepository clientRegistrationRepository) {
		this(clientRegistrationRepository, new PathPatternParserServerWebExchangeMatcher(
				DEFAULT_AUTHORIZATION_REQUEST_PATTERN));
	}

	/**
	 * Creates a new instance
	 * @param clientRegistrationRepository the repository to resolve the {@link ClientRegistration}
	 * @param authorizationRequestMatcher the matcher that determines if the request is a match and extracts the
	 * {@link #DEFAULT_REGISTRATION_ID_URI_VARIABLE_NAME} from the path variables.
	 */
	public DefaultServerOAuth2AuthorizationRequestResolver(ReactiveClientRegistrationRepository clientRegistrationRepository,
			ServerWebExchangeMatcher authorizationRequestMatcher) {
		Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
		Assert.notNull(authorizationRequestMatcher, "authorizationRequestMatcher cannot be null");
		this.clientRegistrationRepository = clientRegistrationRepository;
		this.authorizationRequestMatcher = authorizationRequestMatcher;
	}

	@Override
	public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange) {
		return this.authorizationRequestMatcher.matches(exchange)
				.filter(matchResult -> matchResult.isMatch())
				.map(ServerWebExchangeMatcher.MatchResult::getVariables)
				.map(variables -> variables.get(DEFAULT_REGISTRATION_ID_URI_VARIABLE_NAME))
				.cast(String.class)
				.flatMap(clientRegistrationId -> resolve(exchange, clientRegistrationId));
	}

	@Override
	public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange,
			String clientRegistrationId) {
		return this.findByRegistrationId(exchange, clientRegistrationId)
			.map(clientRegistration -> authorizationRequest(exchange, clientRegistration));
	}

	private Mono<ClientRegistration> findByRegistrationId(ServerWebExchange exchange, String clientRegistration) {
		return this.clientRegistrationRepository.findByRegistrationId(clientRegistration)
				.switchIfEmpty(Mono.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid client registration id")));
	}

	private OAuth2AuthorizationRequest authorizationRequest(ServerWebExchange exchange,
			ClientRegistration clientRegistration) {
		String redirectUriStr = this
					.expandRedirectUri(exchange.getRequest(), clientRegistration);

		Map<String, Object> additionalParameters = new HashMap<>();
		additionalParameters.put(OAuth2ParameterNames.REGISTRATION_ID,
				clientRegistration.getRegistrationId());

		OAuth2AuthorizationRequest.Builder builder;
		if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(clientRegistration.getAuthorizationGrantType())) {
			builder = OAuth2AuthorizationRequest.authorizationCode();
		}
		else if (AuthorizationGrantType.IMPLICIT.equals(clientRegistration.getAuthorizationGrantType())) {
			builder = OAuth2AuthorizationRequest.implicit();
		}
		else {
			throw new IllegalArgumentException(
					"Invalid Authorization Grant Type (" + clientRegistration.getAuthorizationGrantType().getValue()
							+ ") for Client Registration with Id: " + clientRegistration.getRegistrationId());
		}
		return builder
				.clientId(clientRegistration.getClientId())
				.authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
				.redirectUri(redirectUriStr).scopes(clientRegistration.getScopes())
				.state(this.stateGenerator.generateKey())
				.additionalParameters(additionalParameters)
				.build();
	}

	private String expandRedirectUri(ServerHttpRequest request, ClientRegistration clientRegistration) {
		// Supported URI variables -> baseUrl, action, registrationId
		// Used in -> CommonOAuth2Provider.DEFAULT_REDIRECT_URL = "{baseUrl}/{action}/oauth2/code/{registrationId}"
		Map<String, String> uriVariables = new HashMap<>();
		uriVariables.put("registrationId", clientRegistration.getRegistrationId());

		String baseUrl = UriComponentsBuilder.fromHttpRequest(new ServerHttpRequestDecorator(request))
				.replacePath(request.getPath().contextPath().value())
				.replaceQuery(null)
				.build()
				.toUriString();
		uriVariables.put("baseUrl", baseUrl);

		if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(clientRegistration.getAuthorizationGrantType())) {
			String loginAction = "login";
			uriVariables.put("action", loginAction);
		}

		return UriComponentsBuilder.fromUriString(clientRegistration.getRedirectUriTemplate())
				.buildAndExpand(uriVariables)
				.toUriString();
	}
}
