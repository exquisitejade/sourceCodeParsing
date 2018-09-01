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
package org.springframework.security.oauth2.server.resource.authentication;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link JwtAuthenticationProvider}
 *
 * @author Josh Cummings
 */
@RunWith(MockitoJUnitRunner.class)
public class JwtAuthenticationProviderTests {
	@Mock
	Converter<Jwt, JwtAuthenticationToken> jwtAuthenticationConverter;

	@Mock
	JwtDecoder jwtDecoder;

	JwtAuthenticationProvider provider;

	@Before
	public void setup() {
		this.provider =
				new JwtAuthenticationProvider(this.jwtDecoder);
		this.provider.setJwtAuthenticationConverter(jwtAuthenticationConverter);
	}

	@Test
	public void authenticateWhenJwtDecodesThenAuthenticationHasAttributesContainedInJwt() {
		BearerTokenAuthenticationToken token = this.authentication();

		Map<String, Object> claims = new HashMap<>();
		claims.put("name", "value");
		Jwt jwt = this.jwt(claims);

		when(this.jwtDecoder.decode("token")).thenReturn(jwt);
		when(this.jwtAuthenticationConverter.convert(jwt)).thenReturn(new JwtAuthenticationToken(jwt));

		JwtAuthenticationToken authentication =
				(JwtAuthenticationToken) this.provider.authenticate(token);

		assertThat(authentication.getTokenAttributes()).isEqualTo(claims);
	}

	@Test
	public void authenticateWhenJwtDecodeFailsThenRespondsWithInvalidToken() {
		BearerTokenAuthenticationToken token = this.authentication();

		when(this.jwtDecoder.decode("token")).thenThrow(JwtException.class);

		assertThatCode(() -> this.provider.authenticate(token))
				.matches(failed -> failed instanceof OAuth2AuthenticationException)
				.matches(errorCode(BearerTokenErrorCodes.INVALID_TOKEN));
	}

	@Test
	public void authenticateWhenDecoderThrowsIncompatibleErrorMessageThenWrapsWithGenericOne() {
		BearerTokenAuthenticationToken token = this.authentication();

		when(this.jwtDecoder.decode(token.getToken())).thenThrow(new JwtException("with \"invalid\" chars"));

		assertThatCode(() -> this.provider.authenticate(token))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.hasFieldOrPropertyWithValue(
						"error.description",
						"An error occurred while attempting to decode the Jwt: Invalid token");
	}

	@Test
	public void authenticateWhenConverterReturnsAuthenticationThenProviderPropagatesIt() {
		BearerTokenAuthenticationToken token = this.authentication();
		Object details = mock(Object.class);
		token.setDetails(details);

		Jwt jwt = this.jwt(Collections.singletonMap("some", "value"));
		JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

		when(this.jwtDecoder.decode(token.getToken())).thenReturn(jwt);
		when(this.jwtAuthenticationConverter.convert(jwt)).thenReturn(authentication);

		assertThat(this.provider.authenticate(token))
				.isEqualTo(authentication)
				.hasFieldOrPropertyWithValue("details", details);
	}

	@Test
	public void supportsWhenBearerTokenAuthenticationTokenThenReturnsTrue() {
		assertThat(this.provider.supports(BearerTokenAuthenticationToken.class)).isTrue();
	}

	private BearerTokenAuthenticationToken authentication() {
		return new BearerTokenAuthenticationToken("token");
	}

	private Jwt jwt(Map<String, Object> claims) {
		Map<String, Object> headers = new HashMap<>();
		headers.put("alg", JwsAlgorithms.RS256);

		return new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
	}

	private Predicate<? super Throwable> errorCode(String errorCode) {
		return failed ->
				((OAuth2AuthenticationException) failed).getError().getErrorCode() == errorCode;
	}
}
