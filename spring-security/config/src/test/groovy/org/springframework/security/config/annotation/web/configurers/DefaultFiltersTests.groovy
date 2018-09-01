/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.security.config.annotation.web.configurers

import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.config.annotation.BaseSpringSpec
import org.springframework.security.config.annotation.ObjectPostProcessor
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.WebSecurityConfigurer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.BaseWebConfig
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.FilterChainProxy
import org.springframework.security.web.access.ExceptionTranslationFilter
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.logout.LogoutFilter
import org.springframework.security.web.context.SecurityContextPersistenceFilter
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.header.HeaderWriterFilter
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter
import org.springframework.security.web.session.SessionManagementFilter
import org.springframework.security.web.util.matcher.AnyRequestMatcher

/**
 *
 * @author Rob Winch
 */
class DefaultFiltersTests extends BaseSpringSpec {

	def "Default the WebSecurityConfigurerAdapter"() {
		when:
		context = new AnnotationConfigApplicationContext(FilterChainProxyBuilderMissingConfig)
		then:
		context.getBean(FilterChainProxy) != null
	}

	@EnableWebSecurity
	static class FilterChainProxyBuilderMissingConfig {
		@Autowired
		public void configureGlobal(AuthenticationManagerBuilder auth) {
			auth
				.inMemoryAuthentication()
					.withUser("user").password("password").roles("USER")
		}
	}

	@EnableWebSecurity
	static class FilterChainProxyBuilderNoSecurityFilterBuildersConfig {
		@Bean
		public WebSecurity filterChainProxyBuilder(ObjectPostProcessor<Object> opp) {
			new WebSecurity(opp)
				.ignoring()
					.antMatchers("/resources/**")
		}
	}

	def "null WebInvocationPrivilegeEvaluator"() {
		when:
		context = new AnnotationConfigApplicationContext(NullWebInvocationPrivilegeEvaluatorConfig)
		then:
		List<DefaultSecurityFilterChain> filterChains = context.getBean(FilterChainProxy).filterChains
		filterChains.size() == 1
		filterChains[0].requestMatcher instanceof AnyRequestMatcher
		filterChains[0].filters.size() == 1
		filterChains[0].filters.find { it instanceof UsernamePasswordAuthenticationFilter }
	}

	@EnableWebSecurity
	static class NullWebInvocationPrivilegeEvaluatorConfig extends BaseWebConfig {
		NullWebInvocationPrivilegeEvaluatorConfig() {
			super(true)
		}

		protected void configure(HttpSecurity http) {
			http.formLogin()
		}
	}

	def "FilterChainProxyBuilder ignoring resources"() {
		when:
			loadConfig(FilterChainProxyBuilderIgnoringConfig)
		then:
			List<DefaultSecurityFilterChain> filterChains = context.getBean(FilterChainProxy).filterChains
			filterChains.size() == 2
			filterChains[0].requestMatcher.pattern == '/resources/**'
			filterChains[0].filters.empty
			filterChains[1].requestMatcher instanceof AnyRequestMatcher
			filterChains[1].filters.collect { it.class } ==
					[WebAsyncManagerIntegrationFilter, SecurityContextPersistenceFilter, HeaderWriterFilter, CsrfFilter, LogoutFilter, RequestCacheAwareFilter,
					 SecurityContextHolderAwareRequestFilter, AnonymousAuthenticationFilter, SessionManagementFilter,
					 ExceptionTranslationFilter, FilterSecurityInterceptor ]
	}

	@EnableWebSecurity
	static class FilterChainProxyBuilderIgnoringConfig extends BaseWebConfig {
		@Override
		public void configure(WebSecurity builder)	throws Exception {
			builder
				.ignoring()
					.antMatchers("/resources/**");
		}
		@Override
		protected void configure(HttpSecurity http) {
			http
				.authorizeRequests()
					.anyRequest().hasRole("USER");
		}
	}

   def "DefaultFilters.permitAll()"() {
		when:
			loadConfig(DefaultFiltersConfigPermitAll)
			MockHttpServletResponse response = new MockHttpServletResponse()
			request = new MockHttpServletRequest(servletPath : uri, queryString: query, method:"POST")
			setupCsrf()
			springSecurityFilterChain.doFilter(request, response, new MockFilterChain())
		then:
			response.redirectedUrl == "/login?logout"
		where:
			uri | query
			"/logout" | null
	}

	@EnableWebSecurity
	static class DefaultFiltersConfigPermitAll extends BaseWebConfig {
		protected void configure(HttpSecurity http) {
		}
	}
}
