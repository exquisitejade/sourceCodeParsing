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
package org.springframework.security.oauth2.client.authentication;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Map;

/**
 * An implementation of an {@link AuthenticationProvider} for OAuth 2.0 Login,
 * which leverages the OAuth 2.0 Authorization Code Grant Flow.
 *
 * This {@link AuthenticationProvider} is responsible for authenticating
 * an Authorization Code credential with the Authorization Server's Token Endpoint
 * and if valid, exchanging it for an Access Token credential.
 * <p>
 * It will also obtain the user attributes of the End-User (Resource Owner)
 * from the UserInfo Endpoint using an {@link OAuth2UserService},
 * which will create a {@code Principal} in the form of an {@link OAuth2User}.
 * The {@code OAuth2User} is then associated to the {@link OAuth2LoginAuthenticationToken}
 * to complete the authentication.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see OAuth2LoginAuthenticationToken
 * @see OAuth2AccessTokenResponseClient
 * @see OAuth2UserService
 * @see OAuth2User
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-4.1">Section 4.1 Authorization Code Grant Flow</a>
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-4.1.3">Section 4.1.3 Access Token Request</a>
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-4.1.4">Section 4.1.4 Access Token Response</a>
 */
public class OAuth2LoginAuthenticationProvider implements AuthenticationProvider {
	private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;
	private final OAuth2UserService<OAuth2UserRequest, OAuth2User> userService;
	private GrantedAuthoritiesMapper authoritiesMapper = (authorities -> authorities);

	/**
	 * Constructs an {@code OAuth2LoginAuthenticationProvider} using the provided parameters.
	 *
	 * @param accessTokenResponseClient the client used for requesting the access token credential from the Token Endpoint
	 * @param userService the service used for obtaining the user attributes of the End-User from the UserInfo Endpoint
	 */
	public OAuth2LoginAuthenticationProvider(
		OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient,
		OAuth2UserService<OAuth2UserRequest, OAuth2User> userService) {

		Assert.notNull(accessTokenResponseClient, "accessTokenResponseClient cannot be null");
		Assert.notNull(userService, "userService cannot be null");
		this.accessTokenResponseClient = accessTokenResponseClient;
		this.userService = userService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		OAuth2LoginAuthenticationToken authorizationCodeAuthentication =
			(OAuth2LoginAuthenticationToken) authentication;

		// Section 3.1.2.1 Authentication Request - http://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
		// scope
		// 		REQUIRED. OpenID Connect requests MUST contain the "openid" scope value.
		if (authorizationCodeAuthentication.getAuthorizationExchange()
			.getAuthorizationRequest().getScopes().contains("openid")) {
			// This is an OpenID Connect Authentication Request so return null
			// and let OidcAuthorizationCodeAuthenticationProvider handle it instead
			return null;
		}

		OAuth2AuthorizationExchangeValidator.validate(
			authorizationCodeAuthentication.getAuthorizationExchange());

		OAuth2AccessTokenResponse accessTokenResponse =
			this.accessTokenResponseClient.getTokenResponse(
				new OAuth2AuthorizationCodeGrantRequest(
					authorizationCodeAuthentication.getClientRegistration(),
					authorizationCodeAuthentication.getAuthorizationExchange()));

		OAuth2AccessToken accessToken = accessTokenResponse.getAccessToken();
		Map<String, Object> additionalParameters = accessTokenResponse.getAdditionalParameters();

		OAuth2User oauth2User = this.userService.loadUser(new OAuth2UserRequest(
				authorizationCodeAuthentication.getClientRegistration(), accessToken, additionalParameters));

		Collection<? extends GrantedAuthority> mappedAuthorities =
			this.authoritiesMapper.mapAuthorities(oauth2User.getAuthorities());

		OAuth2LoginAuthenticationToken authenticationResult = new OAuth2LoginAuthenticationToken(
			authorizationCodeAuthentication.getClientRegistration(),
			authorizationCodeAuthentication.getAuthorizationExchange(),
			oauth2User,
			mappedAuthorities,
			accessToken,
			accessTokenResponse.getRefreshToken());
		authenticationResult.setDetails(authorizationCodeAuthentication.getDetails());

		return authenticationResult;
	}

	/**
	 * Sets the {@link GrantedAuthoritiesMapper} used for mapping {@link OAuth2User#getAuthorities()}
	 * to a new set of authorities which will be associated to the {@link OAuth2LoginAuthenticationToken}.
	 *
	 * @param authoritiesMapper the {@link GrantedAuthoritiesMapper} used for mapping the user's authorities
	 */
	public final void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
		Assert.notNull(authoritiesMapper, "authoritiesMapper cannot be null");
		this.authoritiesMapper = authoritiesMapper;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return OAuth2LoginAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
