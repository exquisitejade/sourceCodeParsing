/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.freemarker;

import java.io.StringWriter;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FreeMarkerAutoConfiguration} Servlet support.
 *
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 */
public class FreeMarkerAutoConfigurationServletIntegrationTests {

	private AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

	@Before
	public void setupContext() {
		this.context.setServletContext(new MockServletContext());
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultConfiguration() {
		registerAndRefreshContext();
		assertThat(this.context.getBean(FreeMarkerViewResolver.class)).isNotNull();
		assertThat(this.context.getBean(FreeMarkerConfigurer.class)).isNotNull();
		assertThat(this.context.getBean(FreeMarkerConfig.class)).isNotNull();
		assertThat(this.context.getBean(freemarker.template.Configuration.class))
				.isNotNull();
	}

	@Test
	public void defaultViewResolution() throws Exception {
		registerAndRefreshContext();
		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();
		assertThat(result).contains("home");
		assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
	}

	@Test
	public void customContentType() throws Exception {
		registerAndRefreshContext("spring.freemarker.contentType:application/json");
		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();
		assertThat(result).contains("home");
		assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
	}

	@Test
	public void customPrefix() throws Exception {
		registerAndRefreshContext("spring.freemarker.prefix:prefix/");
		MockHttpServletResponse response = render("prefixed");
		String result = response.getContentAsString();
		assertThat(result).contains("prefixed");
	}

	@Test
	public void customSuffix() throws Exception {
		registerAndRefreshContext("spring.freemarker.suffix:.freemarker");
		MockHttpServletResponse response = render("suffixed");
		String result = response.getContentAsString();
		assertThat(result).contains("suffixed");
	}

	@Test
	public void customTemplateLoaderPath() throws Exception {
		registerAndRefreshContext(
				"spring.freemarker.templateLoaderPath:classpath:/custom-templates/");
		MockHttpServletResponse response = render("custom");
		String result = response.getContentAsString();
		assertThat(result).contains("custom");
	}

	@Test
	public void disableCache() {
		registerAndRefreshContext("spring.freemarker.cache:false");
		assertThat(this.context.getBean(FreeMarkerViewResolver.class).getCacheLimit())
				.isEqualTo(0);
	}

	@Test
	public void allowSessionOverride() {
		registerAndRefreshContext("spring.freemarker.allow-session-override:true");
		AbstractTemplateViewResolver viewResolver = this.context
				.getBean(FreeMarkerViewResolver.class);
		assertThat(ReflectionTestUtils.getField(viewResolver, "allowSessionOverride"))
				.isEqualTo(true);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void customFreeMarkerSettings() {
		registerAndRefreshContext("spring.freemarker.settings.boolean_format:yup,nope");
		assertThat(this.context.getBean(FreeMarkerConfigurer.class).getConfiguration()
				.getSetting("boolean_format")).isEqualTo("yup,nope");
	}

	@Test
	public void renderTemplate() throws Exception {
		registerAndRefreshContext();
		FreeMarkerConfigurer freemarker = this.context
				.getBean(FreeMarkerConfigurer.class);
		StringWriter writer = new StringWriter();
		freemarker.getConfiguration().getTemplate("message.ftl").process(this, writer);
		assertThat(writer.toString()).contains("Hello World");
	}

	@Test
	public void registerResourceHandlingFilterDisabledByDefault() {
		registerAndRefreshContext();
		assertThat(this.context.getBeansOfType(ResourceUrlEncodingFilter.class))
				.isEmpty();
	}

	@Test
	public void registerResourceHandlingFilterOnlyIfResourceChainIsEnabled() {
		registerAndRefreshContext("spring.resources.chain.enabled:true");
		assertThat(this.context.getBean(ResourceUrlEncodingFilter.class)).isNotNull();
	}

	private void registerAndRefreshContext(String... env) {
		TestPropertyValues.of(env).applyTo(this.context);
		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();
	}

	public String getGreeting() {
		return "Hello World";
	}

	private MockHttpServletResponse render(String viewName) throws Exception {
		FreeMarkerViewResolver resolver = this.context
				.getBean(FreeMarkerViewResolver.class);
		View view = resolver.resolveViewName(viewName, Locale.UK);
		assertThat(view).isNotNull();
		HttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE,
				this.context);
		MockHttpServletResponse response = new MockHttpServletResponse();
		view.render(null, request, response);
		return response;
	}

}
