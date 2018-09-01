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

package org.springframework.security.test.context.support;

import org.reactivestreams.Subscription;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ClassUtils;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

/**
 * Sets up the Reactor Context with the Authentication from the TestSecurityContextHolder
 * and then clears the Reactor Context at the end of the tests.
 *
 * @author Rob Winch
 * @since 5.0
 * @see WithSecurityContextTestExecutionListener
 * @see org.springframework.security.test.context.annotation.SecurityTestExecutionListeners
 */
public class ReactorContextTestExecutionListener
	extends DelegatingTestExecutionListener {

	private static final String HOOKS_CLASS_NAME = "reactor.core.publisher.Hooks";

	public ReactorContextTestExecutionListener() {
		super(createDelegate());
	}

	private static TestExecutionListener createDelegate() {
		return ClassUtils.isPresent(HOOKS_CLASS_NAME, ReactorContextTestExecutionListener.class.getClassLoader()) ?
			new DelegateTestExecutionListener() :
			new AbstractTestExecutionListener() {};
	}

	private static class DelegateTestExecutionListener extends AbstractTestExecutionListener {
		@Override
		public void beforeTestMethod(TestContext testContext) throws Exception {
			SecurityContext securityContext = TestSecurityContextHolder.getContext();
			Hooks.onLastOperator(Operators.lift((s, sub) -> new SecuritySubContext<>(sub, securityContext)));
		}

		@Override
		public void afterTestMethod(TestContext testContext) throws Exception {
			Hooks.resetOnLastOperator();
		}

		private static class SecuritySubContext<T> implements CoreSubscriber<T> {
			private static String CONTEXT_DEFAULTED_ATTR_NAME = SecuritySubContext.class.getName().concat(".CONTEXT_DEFAULTED_ATTR_NAME");

			private final CoreSubscriber<T> delegate;
			private final SecurityContext securityContext;

			SecuritySubContext(CoreSubscriber<T> delegate, SecurityContext securityContext) {
				this.delegate = delegate;
				this.securityContext = securityContext;
			}

			@Override
			public Context currentContext() {
				Context context = delegate.currentContext();
				if (context.hasKey(CONTEXT_DEFAULTED_ATTR_NAME)) {
					return context;
				}
				context = context.put(CONTEXT_DEFAULTED_ATTR_NAME, Boolean.TRUE);
				Authentication authentication = securityContext.getAuthentication();
				if (authentication == null) {
					return context;
				}
				Context toMerge = ReactiveSecurityContextHolder.withSecurityContext(
						Mono.just(this.securityContext));
				return toMerge.putAll(context);
			}

			@Override
			public void onSubscribe(Subscription s) {
				delegate.onSubscribe(s);
			}

			@Override
			public void onNext(T t) {
				delegate.onNext(t);
			}

			@Override
			public void onError(Throwable t) {
				delegate.onError(t);
			}

			@Override
			public void onComplete() {
				delegate.onComplete();
			}
		}
	}

	/**
	 * Returns {@code 11000}.
	 */
	@Override
	public int getOrder() {
		return 11000;
	}
}
