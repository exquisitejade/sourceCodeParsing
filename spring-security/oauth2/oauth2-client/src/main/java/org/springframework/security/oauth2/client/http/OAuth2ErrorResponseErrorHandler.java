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
package org.springframework.security.oauth2.client.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

/**
 * A {@link ResponseErrorHandler} that handles an {@link OAuth2Error OAuth 2.0 Error}.
 *
 * @see ResponseErrorHandler
 * @see OAuth2Error
 * @author Joe Grandja
 * @since 5.1
 */
public class OAuth2ErrorResponseErrorHandler implements ResponseErrorHandler {
	private final OAuth2ErrorHttpMessageConverter oauth2ErrorConverter = new OAuth2ErrorHttpMessageConverter();
	private final ResponseErrorHandler defaultErrorHandler = new DefaultResponseErrorHandler();

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return this.defaultErrorHandler.hasError(response);
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		if (HttpStatus.BAD_REQUEST.equals(response.getStatusCode())) {
			OAuth2Error oauth2Error = this.oauth2ErrorConverter.read(OAuth2Error.class, response);
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
		}
		this.defaultErrorHandler.handleError(response);
	}
}
