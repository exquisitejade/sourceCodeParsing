/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.security.test.web.servlet.setup;

import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.security.config.BeanIds;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SecurityMockMvcConfigurerTests {
	@Mock
	private Filter filter;
	@Mock
	private Filter beanFilter;
	@Mock
	private ConfigurableMockMvcBuilder<?> builder;
	@Mock
	private WebApplicationContext context;
	@Mock
	private ServletContext servletContext;

	@Before
	public void setup() {
		when(this.context.getServletContext()).thenReturn(this.servletContext);
	}

	@Test
	public void beforeMockMvcCreatedOverrideBean() throws Exception {
		returnFilterBean();
		SecurityMockMvcConfigurer configurer = new SecurityMockMvcConfigurer(this.filter);

		configurer.beforeMockMvcCreated(this.builder, this.context);

		verify(this.builder).addFilters(this.filter);
		verify(this.servletContext).setAttribute(BeanIds.SPRING_SECURITY_FILTER_CHAIN,
				this.filter);
	}

	@Test
	public void beforeMockMvcCreatedBean() throws Exception {
		returnFilterBean();
		SecurityMockMvcConfigurer configurer = new SecurityMockMvcConfigurer();

		configurer.beforeMockMvcCreated(this.builder, this.context);

		verify(this.builder).addFilters(this.beanFilter);
	}

	@Test
	public void beforeMockMvcCreatedNoBean() throws Exception {
		SecurityMockMvcConfigurer configurer = new SecurityMockMvcConfigurer(this.filter);

		configurer.beforeMockMvcCreated(this.builder, this.context);

		verify(this.builder).addFilters(this.filter);
	}

	@Test(expected = IllegalStateException.class)
	public void beforeMockMvcCreatedNoFilter() throws Exception {
		SecurityMockMvcConfigurer configurer = new SecurityMockMvcConfigurer();

		configurer.beforeMockMvcCreated(this.builder, this.context);
	}

	private void returnFilterBean() {
		when(this.context.containsBean(anyString())).thenReturn(true);
		when(this.context.getBean(anyString(), eq(Filter.class)))
				.thenReturn(this.beanFilter);
	}
}
