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
package org.springframework.security.oauth2.client;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.util.Assert;

import reactor.core.publisher.Mono;

/**
 * An {@link OAuth2AuthorizedClientService} that stores
 * {@link OAuth2AuthorizedClient Authorized Client(s)} in-memory.
 *
 * @author Rob Winch
 * @since 5.1
 * @see OAuth2AuthorizedClientService
 * @see OAuth2AuthorizedClient
 * @see ClientRegistration
 * @see Authentication
 */
public final class InMemoryReactiveOAuth2AuthorizedClientService implements ReactiveOAuth2AuthorizedClientService {
	private final Map<String, OAuth2AuthorizedClient> authorizedClients = new ConcurrentHashMap<>();
	private final ReactiveClientRegistrationRepository clientRegistrationRepository;

	/**
	 * Constructs an {@code InMemoryOAuth2AuthorizedClientService} using the provided parameters.
	 *
	 * @param clientRegistrationRepository the repository of client registrations
	 */
	public InMemoryReactiveOAuth2AuthorizedClientService(ReactiveClientRegistrationRepository clientRegistrationRepository) {
		Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	@Override
	public <T extends OAuth2AuthorizedClient> Mono<T> loadAuthorizedClient(String clientRegistrationId, String principalName) {
		Assert.hasText(clientRegistrationId, "clientRegistrationId cannot be empty");
		Assert.hasText(principalName, "principalName cannot be empty");
		return (Mono<T>) getIdentifier(clientRegistrationId, principalName)
				.flatMap(identifier -> Mono.justOrEmpty(this.authorizedClients.get(identifier)));
	}

	@Override
	public Mono<Void> saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
		Assert.notNull(authorizedClient, "authorizedClient cannot be null");
		Assert.notNull(principal, "principal cannot be null");
		return Mono.fromRunnable(() -> {
			String identifier = this.getIdentifier(authorizedClient.getClientRegistration(), principal.getName());
			this.authorizedClients.put(identifier, authorizedClient);
		});
	}

	@Override
	public Mono<Void> removeAuthorizedClient(String clientRegistrationId, String principalName) {
		Assert.hasText(clientRegistrationId, "clientRegistrationId cannot be empty");
		Assert.hasText(principalName, "principalName cannot be empty");
		return this.getIdentifier(clientRegistrationId, principalName)
				.doOnNext(identifier -> this.authorizedClients.remove(identifier))
				.then(Mono.empty());
	}

	private Mono<String> getIdentifier(String clientRegistrationId, String principalName) {
		return this.clientRegistrationRepository.findByRegistrationId(clientRegistrationId)
				.map(registration -> getIdentifier(registration, principalName));
	}

	private String getIdentifier(ClientRegistration registration, String principalName) {
		String identifier = "[" + registration.getRegistrationId() + "][" + principalName + "]";
		return Base64.getEncoder().encodeToString(identifier.getBytes());
	}
}
