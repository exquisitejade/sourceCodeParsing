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
package org.springframework.security.oauth2.client.endpoint;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link DefaultAuthorizationCodeTokenResponseClient}.
 *
 * @author Joe Grandja
 */
public class DefaultAuthorizationCodeTokenResponseClientTests {
	private DefaultAuthorizationCodeTokenResponseClient tokenResponseClient =
			new DefaultAuthorizationCodeTokenResponseClient();
	private ClientRegistration clientRegistration;
	private MockWebServer server;

	@Before
	public void setup() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String tokenUri = this.server.url("/oauth2/token").toString();
		this.clientRegistration = ClientRegistration.withRegistrationId("registration-1")
				.clientId("client-1")
				.clientSecret("secret")
				.clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUriTemplate("https://client.com/callback/client-1")
				.scope("read", "write")
				.authorizationUri("https://provider.com/oauth2/authorize")
				.tokenUri(tokenUri)
				.userInfoUri("https://provider.com/user")
				.userNameAttributeName("id")
				.clientName("client-1")
				.build();
	}

	@After
	public void cleanup() throws Exception {
		this.server.shutdown();
	}

	@Test
	public void setRequestEntityConverterWhenConverterIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.tokenResponseClient.setRequestEntityConverter(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void setRestOperationsWhenRestOperationsIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.tokenResponseClient.setRestOperations(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void getTokenResponseWhenRequestIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.tokenResponseClient.getTokenResponse(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void getTokenResponseWhenSuccessResponseThenReturnAccessTokenResponse() throws Exception {
		String accessTokenSuccessResponse = "{\n" +
				"	\"access_token\": \"access-token-1234\",\n" +
				"   \"token_type\": \"bearer\",\n" +
				"   \"expires_in\": \"3600\",\n" +
				"   \"scope\": \"read write\",\n" +
				"   \"refresh_token\": \"refresh-token-1234\",\n" +
				"   \"custom_parameter_1\": \"custom-value-1\",\n" +
				"   \"custom_parameter_2\": \"custom-value-2\"\n" +
				"}\n";
		this.server.enqueue(jsonResponse(accessTokenSuccessResponse));

		Instant expiresAtBefore = Instant.now().plusSeconds(3600);

		OAuth2AccessTokenResponse accessTokenResponse =
				this.tokenResponseClient.getTokenResponse(this.authorizationCodeGrantRequest());

		Instant expiresAtAfter = Instant.now().plusSeconds(3600);

		RecordedRequest recordedRequest = this.server.takeRequest();
		assertThat(recordedRequest.getMethod()).isEqualTo(HttpMethod.POST.toString());
		assertThat(recordedRequest.getHeader(HttpHeaders.ACCEPT)).isEqualTo(MediaType.APPLICATION_JSON_UTF8_VALUE);
		assertThat(recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED_VALUE + ";charset=UTF-8");

		String formParameters = recordedRequest.getBody().readUtf8();
		assertThat(formParameters).contains("grant_type=authorization_code");
		assertThat(formParameters).contains("code=code-1234");
		assertThat(formParameters).contains("redirect_uri=https%3A%2F%2Fclient.com%2Fcallback%2Fclient-1");

		assertThat(accessTokenResponse.getAccessToken().getTokenValue()).isEqualTo("access-token-1234");
		assertThat(accessTokenResponse.getAccessToken().getTokenType()).isEqualTo(OAuth2AccessToken.TokenType.BEARER);
		assertThat(accessTokenResponse.getAccessToken().getExpiresAt()).isBetween(expiresAtBefore, expiresAtAfter);
		assertThat(accessTokenResponse.getAccessToken().getScopes()).containsExactly("read", "write");
		assertThat(accessTokenResponse.getRefreshToken().getTokenValue()).isEqualTo("refresh-token-1234");
		assertThat(accessTokenResponse.getAdditionalParameters().size()).isEqualTo(2);
		assertThat(accessTokenResponse.getAdditionalParameters()).containsEntry("custom_parameter_1", "custom-value-1");
		assertThat(accessTokenResponse.getAdditionalParameters()).containsEntry("custom_parameter_2", "custom-value-2");
	}

	@Test
	public void getTokenResponseWhenClientAuthenticationBasicThenAuthorizationHeaderIsSent() throws Exception {
		String accessTokenSuccessResponse = "{\n" +
				"	\"access_token\": \"access-token-1234\",\n" +
				"   \"token_type\": \"bearer\",\n" +
				"   \"expires_in\": \"3600\"\n" +
				"}\n";
		this.server.enqueue(jsonResponse(accessTokenSuccessResponse));

		this.tokenResponseClient.getTokenResponse(this.authorizationCodeGrantRequest());

		RecordedRequest recordedRequest = this.server.takeRequest();
		assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).startsWith("Basic ");
	}

	@Test
	public void getTokenResponseWhenClientAuthenticationPostThenFormParametersAreSent() throws Exception {
		String accessTokenSuccessResponse = "{\n" +
				"	\"access_token\": \"access-token-1234\",\n" +
				"   \"token_type\": \"bearer\",\n" +
				"   \"expires_in\": \"3600\"\n" +
				"}\n";
		this.server.enqueue(jsonResponse(accessTokenSuccessResponse));

		ClientRegistration clientRegistration = this.from(this.clientRegistration)
				.clientAuthenticationMethod(ClientAuthenticationMethod.POST)
				.build();

		this.tokenResponseClient.getTokenResponse(this.authorizationCodeGrantRequest(clientRegistration));

		RecordedRequest recordedRequest = this.server.takeRequest();
		assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).isNull();

		String formParameters = recordedRequest.getBody().readUtf8();
		assertThat(formParameters).contains("client_id=client-1");
		assertThat(formParameters).contains("client_secret=secret");
	}

	@Test
	public void getTokenResponseWhenSuccessResponseAndNotBearerTokenTypeThenThrowOAuth2AuthenticationException() {
		String accessTokenSuccessResponse = "{\n" +
				"	\"access_token\": \"access-token-1234\",\n" +
				"   \"token_type\": \"not-bearer\",\n" +
				"   \"expires_in\": \"3600\"\n" +
				"}\n";
		this.server.enqueue(jsonResponse(accessTokenSuccessResponse));

		assertThatThrownBy(() -> this.tokenResponseClient.getTokenResponse(this.authorizationCodeGrantRequest()))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.hasMessageContaining("[invalid_token_response] An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response")
				.hasMessageContaining("tokenType cannot be null");
	}

	@Test
	public void getTokenResponseWhenSuccessResponseAndMissingTokenTypeParameterThenThrowOAuth2AuthenticationException() {
		String accessTokenSuccessResponse = "{\n" +
				"	\"access_token\": \"access-token-1234\"\n" +
				"}\n";
		this.server.enqueue(jsonResponse(accessTokenSuccessResponse));

		assertThatThrownBy(() -> this.tokenResponseClient.getTokenResponse(this.authorizationCodeGrantRequest()))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.hasMessageContaining("[invalid_token_response] An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response")
				.hasMessageContaining("tokenType cannot be null");
	}

	@Test
	public void getTokenResponseWhenSuccessResponseIncludesScopeThenAccessTokenHasResponseScope() {
		String accessTokenSuccessResponse = "{\n" +
				"	\"access_token\": \"access-token-1234\",\n" +
				"   \"token_type\": \"bearer\",\n" +
				"   \"expires_in\": \"3600\",\n" +
				"   \"refresh_token\": \"refresh-token-1234\",\n" +
				"   \"scope\": \"read\"\n" +
				"}\n";
		this.server.enqueue(jsonResponse(accessTokenSuccessResponse));

		OAuth2AccessTokenResponse accessTokenResponse =
				this.tokenResponseClient.getTokenResponse(this.authorizationCodeGrantRequest());

		assertThat(accessTokenResponse.getAccessToken().getScopes()).containsExactly("read");
	}

	@Test
	public void getTokenResponseWhenSuccessResponseDoesNotIncludeScopeThenAccessTokenHasDefaultScope() {
		String accessTokenSuccessResponse = "{\n" +
				"	\"access_token\": \"access-token-1234\",\n" +
				"   \"token_type\": \"bearer\",\n" +
				"   \"expires_in\": \"3600\",\n" +
				"   \"refresh_token\": \"refresh-token-1234\"\n" +
				"}\n";
		this.server.enqueue(jsonResponse(accessTokenSuccessResponse));

		OAuth2AccessTokenResponse accessTokenResponse =
				this.tokenResponseClient.getTokenResponse(this.authorizationCodeGrantRequest());

		assertThat(accessTokenResponse.getAccessToken().getScopes()).containsExactly("read", "write");
	}

	@Test
	public void getTokenResponseWhenTokenUriInvalidThenThrowOAuth2AuthenticationException() {
		String invalidTokenUri = "http://invalid-provider.com/oauth2/token";
		ClientRegistration clientRegistration = this.from(this.clientRegistration)
				.tokenUri(invalidTokenUri)
				.build();

		assertThatThrownBy(() -> this.tokenResponseClient.getTokenResponse(this.authorizationCodeGrantRequest(clientRegistration)))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.hasMessageContaining("[invalid_token_response] An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response");
	}

	@Test
	public void getTokenResponseWhenMalformedResponseThenThrowOAuth2AuthenticationException() {
		String accessTokenSuccessResponse = "{\n" +
				"	\"access_token\": \"access-token-1234\",\n" +
				"   \"token_type\": \"bearer\",\n" +
				"   \"expires_in\": \"3600\",\n" +
				"   \"scope\": \"read write\",\n" +
				"   \"refresh_token\": \"refresh-token-1234\",\n" +
				"   \"custom_parameter_1\": \"custom-value-1\",\n" +
				"   \"custom_parameter_2\": \"custom-value-2\"\n";
//			"}\n";		// Make the JSON invalid/malformed
		this.server.enqueue(jsonResponse(accessTokenSuccessResponse));

		assertThatThrownBy(() -> this.tokenResponseClient.getTokenResponse(this.authorizationCodeGrantRequest()))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.hasMessageContaining("[invalid_token_response] An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response");
	}

	@Test
	public void getTokenResponseWhenErrorResponseThenThrowOAuth2AuthenticationException() {
		String accessTokenErrorResponse = "{\n" +
				"   \"error\": \"unauthorized_client\"\n" +
				"}\n";
		this.server.enqueue(jsonResponse(accessTokenErrorResponse).setResponseCode(400));

		assertThatThrownBy(() -> this.tokenResponseClient.getTokenResponse(this.authorizationCodeGrantRequest()))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.hasMessageContaining("[unauthorized_client]");
	}

	@Test
	public void getTokenResponseWhenServerErrorResponseThenThrowOAuth2AuthenticationException() {
		this.server.enqueue(new MockResponse().setResponseCode(500));

		assertThatThrownBy(() -> this.tokenResponseClient.getTokenResponse(this.authorizationCodeGrantRequest()))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.hasMessage("[invalid_token_response] An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response: 500 Server Error");
	}

	private OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest() {
		return this.authorizationCodeGrantRequest(this.clientRegistration);
	}

	private OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest(
			ClientRegistration clientRegistration) {
		OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest
				.authorizationCode()
				.clientId(clientRegistration.getClientId())
				.state("state-1234")
				.authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
				.redirectUri(clientRegistration.getRedirectUriTemplate())
				.scopes(clientRegistration.getScopes())
				.build();
		OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse
				.success("code-1234")
				.state("state-1234")
				.redirectUri(clientRegistration.getRedirectUriTemplate())
				.build();
		OAuth2AuthorizationExchange authorizationExchange =
				new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);
		return new OAuth2AuthorizationCodeGrantRequest(clientRegistration, authorizationExchange);
	}

	private MockResponse jsonResponse(String json) {
		return new MockResponse()
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(json);
	}

	private ClientRegistration.Builder from(ClientRegistration registration) {
		return ClientRegistration.withRegistrationId(registration.getRegistrationId())
				.clientId(registration.getClientId())
				.clientSecret(registration.getClientSecret())
				.clientAuthenticationMethod(registration.getClientAuthenticationMethod())
				.authorizationGrantType(registration.getAuthorizationGrantType())
				.redirectUriTemplate(registration.getRedirectUriTemplate())
				.scope(registration.getScopes())
				.authorizationUri(registration.getProviderDetails().getAuthorizationUri())
				.tokenUri(registration.getProviderDetails().getTokenUri())
				.userInfoUri(registration.getProviderDetails().getUserInfoEndpoint().getUri())
				.userNameAttributeName(registration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName())
				.clientName(registration.getClientName());
	}
}
