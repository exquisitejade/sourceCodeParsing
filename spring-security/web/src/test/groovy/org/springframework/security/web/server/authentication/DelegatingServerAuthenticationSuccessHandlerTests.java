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

package org.springframework.security.web.server.authentication;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;

import reactor.test.publisher.PublisherProbe;

/**
 * @author Rob Winch
 * @since 5.1
 */
@RunWith(MockitoJUnitRunner.class)
public class DelegatingServerAuthenticationSuccessHandlerTests {
	@Mock
	private ServerAuthenticationSuccessHandler delegate1;

	@Mock
	private ServerAuthenticationSuccessHandler delegate2;
	private PublisherProbe<Void> delegate1Result = PublisherProbe.empty();
	private PublisherProbe<Void> delegate2Result = PublisherProbe.empty();

	@Mock
	private WebFilterExchange exchange;

	@Mock
	private Authentication authentication;

	@Before
	public void setup() {
		when(this.delegate1.onAuthenticationSuccess(any(), any())).thenReturn(this.delegate1Result.mono());
		when(this.delegate2.onAuthenticationSuccess(any(), any())).thenReturn(this.delegate2Result.mono());
	}

	@Test
	public void constructorWhenNullThenIllegalArgumentException() {
		assertThatThrownBy(() -> new DelegatingServerAuthenticationSuccessHandler((ServerAuthenticationSuccessHandler[]) null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void constructorWhenEmptyThenIllegalArgumentException() {
		assertThatThrownBy(() -> new DelegatingServerAuthenticationSuccessHandler(new ServerAuthenticationSuccessHandler[0]))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void onAuthenticationSuccessWhenSingleThenExecuted() {
		DelegatingServerAuthenticationSuccessHandler handler = new DelegatingServerAuthenticationSuccessHandler(
				this.delegate1);

		handler.onAuthenticationSuccess(this.exchange, this.authentication).block();

		this.delegate1Result.assertWasSubscribed();
	}

	@Test
	public void onAuthenticationSuccessWhenMultipleThenExecuted() {
		DelegatingServerAuthenticationSuccessHandler handler = new DelegatingServerAuthenticationSuccessHandler(
				this.delegate1, this.delegate2);

		handler.onAuthenticationSuccess(this.exchange, this.authentication).block();

		this.delegate1Result.assertWasSubscribed();
		this.delegate2Result.assertWasSubscribed();
	}
}
