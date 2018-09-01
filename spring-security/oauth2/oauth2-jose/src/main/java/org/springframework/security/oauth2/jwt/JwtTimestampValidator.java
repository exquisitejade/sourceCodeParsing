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
package org.springframework.security.oauth2.jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

/**
 * An implementation of {@see OAuth2TokenValidator} for verifying claims in a Jwt-based access token
 *
 * <p>
 * Because clocks can differ between the Jwt source, say the Authorization Server, and its destination, say the
 * Resource Server, there is a default clock leeway exercised when deciding if the current time is within the Jwt's
 * specified operating window
 *
 * @author Josh Cummings
 * @since 5.1
 * @see Jwt
 * @see OAuth2TokenValidator
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc7519">JSON Web Token (JWT)</a>
 */
public final class JwtTimestampValidator implements OAuth2TokenValidator<Jwt> {
	private static final Duration DEFAULT_MAX_CLOCK_SKEW = Duration.of(60, ChronoUnit.SECONDS);

	private final Duration maxClockSkew;

	private Clock clock = Clock.systemUTC();

	/**
	 * A basic instance with no custom verification and the default max clock skew
	 */
	public JwtTimestampValidator() {
		this(DEFAULT_MAX_CLOCK_SKEW);
	}

	public JwtTimestampValidator(Duration maxClockSkew) {
		Assert.notNull(maxClockSkew, "maxClockSkew cannot be null");

		this.maxClockSkew = maxClockSkew;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		Assert.notNull(jwt, "jwt cannot be null");

		Instant expiry = jwt.getExpiresAt();

		if (expiry != null) {
			if (Instant.now(this.clock).minus(maxClockSkew).isAfter(expiry)) {
				OAuth2Error error = new OAuth2Error(
						OAuth2ErrorCodes.INVALID_REQUEST,
						String.format("Jwt expired at %s", jwt.getExpiresAt()),
						"https://tools.ietf.org/html/rfc6750#section-3.1");
				return OAuth2TokenValidatorResult.failure(error);
			}
		}

		Instant notBefore = jwt.getNotBefore();

		if (notBefore != null) {
			if (Instant.now(this.clock).plus(maxClockSkew).isBefore(notBefore)) {
				OAuth2Error error = new OAuth2Error(
						OAuth2ErrorCodes.INVALID_REQUEST,
						String.format("Jwt used before %s", jwt.getNotBefore()),
						"https://tools.ietf.org/html/rfc6750#section-3.1");
				return OAuth2TokenValidatorResult.failure(error);
			}
		}

		return OAuth2TokenValidatorResult.success();
	}

	/**
	 * '
	 * Use this {@link Clock} with {@link Instant#now()} for assessing
	 * timestamp validity
	 *
	 * @param clock
	 */
	public void setClock(Clock clock) {
		Assert.notNull(clock, "clock cannot be null");
		this.clock = clock;
	}
}
