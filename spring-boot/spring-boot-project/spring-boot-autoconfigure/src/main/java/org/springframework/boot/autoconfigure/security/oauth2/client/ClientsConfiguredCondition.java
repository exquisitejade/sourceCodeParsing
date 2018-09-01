/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that matches if any {@code spring.security.oauth2.client.registration}
 * properties are defined.
 *
 * @author Madhura Bhave
 * @since 2.1.0
 */
public class ClientsConfiguredCondition extends SpringBootCondition {

	private static final Bindable<Map<String, OAuth2ClientProperties.LoginClientRegistration>> STRING_LOGIN_REGISTRATION_MAP = Bindable
			.mapOf(String.class, OAuth2ClientProperties.LoginClientRegistration.class);

	private static final Bindable<Map<String, OAuth2ClientProperties.AuthorizationCodeClientRegistration>> STRING_AUTHORIZATIONCODE_REGISTRATION_MAP = Bindable
			.mapOf(String.class,
					OAuth2ClientProperties.AuthorizationCodeClientRegistration.class);

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage
				.forCondition("OAuth2 Clients Configured Condition");
		Map<String, OAuth2ClientProperties.BaseClientRegistration> registrations = getRegistrations(
				context.getEnvironment());
		if (!registrations.isEmpty()) {
			return ConditionOutcome.match(message.foundExactly(
					"registered clients " + registrations.values().stream().map(
							OAuth2ClientProperties.BaseClientRegistration::getClientId)
							.collect(Collectors.joining(", "))));
		}
		return ConditionOutcome.noMatch(message.notAvailable("registered clients"));
	}

	private Map<String, OAuth2ClientProperties.BaseClientRegistration> getRegistrations(
			Environment environment) {
		Map<String, OAuth2ClientProperties.BaseClientRegistration> registrations = new HashMap();
		Map<String, OAuth2ClientProperties.LoginClientRegistration> loginClientRegistrations = Binder
				.get(environment).bind("spring.security.oauth2.client.registration.login",
						STRING_LOGIN_REGISTRATION_MAP)
				.orElse(Collections.emptyMap());
		Map<String, OAuth2ClientProperties.AuthorizationCodeClientRegistration> authCodeClientRegistrations = Binder
				.get(environment)
				.bind("spring.security.oauth2.client.registration.authorizationcode",
						STRING_AUTHORIZATIONCODE_REGISTRATION_MAP)
				.orElse(Collections.emptyMap());
		registrations.putAll(loginClientRegistrations);
		registrations.putAll(authCodeClientRegistrations);
		return registrations;
	}

}
