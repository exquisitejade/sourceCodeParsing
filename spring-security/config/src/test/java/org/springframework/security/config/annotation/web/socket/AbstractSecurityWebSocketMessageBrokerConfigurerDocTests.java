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
package org.springframework.security.config.annotation.web.socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import static org.springframework.messaging.simp.SimpMessageType.*;

import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;


import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class AbstractSecurityWebSocketMessageBrokerConfigurerDocTests {
	AnnotationConfigWebApplicationContext context;

	TestingAuthenticationToken messageUser;

	CsrfToken token;

	String sessionAttr;

	@Before
	public void setup() {
		token = new DefaultCsrfToken("header", "param", "token");
		sessionAttr = "sessionAttr";
		messageUser = new TestingAuthenticationToken("user", "pass", "ROLE_USER");
	}

	@After
	public void cleanup() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void securityMappings() {
		loadConfig(WebSocketSecurityConfig.class);

		clientInboundChannel().send(
				message("/user/queue/errors", SimpMessageType.SUBSCRIBE));

		try {
			clientInboundChannel().send(message("/denyAll", SimpMessageType.MESSAGE));
			fail("Expected Exception");
		}
		catch (MessageDeliveryException expected) {
			assertThat(expected.getCause()).isInstanceOf(AccessDeniedException.class);
		}
	}

	private void loadConfig(Class<?>... configs) {
		context = new AnnotationConfigWebApplicationContext();
		context.register(configs);
		context.register(WebSocketConfig.class, SyncExecutorConfig.class);
		context.setServletConfig(new MockServletConfig());
		context.refresh();
	}

	private MessageChannel clientInboundChannel() {
		return context.getBean("clientInboundChannel", MessageChannel.class);
	}

	private Message<String> message(String destination, SimpMessageType type) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(type);
		return message(headers, destination);
	}

	private Message<String> message(SimpMessageHeaderAccessor headers, String destination) {
		headers.setSessionId("123");
		headers.setSessionAttributes(new HashMap<>());
		if (destination != null) {
			headers.setDestination(destination);
		}
		if (messageUser != null) {
			headers.setUser(messageUser);
		}
		return new GenericMessage<>("hi", headers.getMessageHeaders());
	}

	@Controller
	static class MyController {

		@MessageMapping("/authentication")
		public void authentication(@AuthenticationPrincipal String un) {
			// ... do something ...
		}
	}

	@Configuration
	static class WebSocketSecurityConfig extends
			AbstractSecurityWebSocketMessageBrokerConfigurer {

		@Override
		protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
			messages.nullDestMatcher().authenticated()
					// <1>
					.simpSubscribeDestMatchers("/user/queue/errors").permitAll()
					// <2>
					.simpDestMatchers("/app/**").hasRole("USER")
					// <3>
					.simpSubscribeDestMatchers("/user/**", "/topic/friends/*")
					.hasRole("USER") // <4>
					.simpTypeMatchers(MESSAGE, SUBSCRIBE).denyAll() // <5>
					.anyMessage().denyAll(); // <6>

		}
	}

	@Configuration
	@EnableWebSocketMessageBroker
	static class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/chat").withSockJS();
		}

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.enableSimpleBroker("/queue/", "/topic/");
			registry.setApplicationDestinationPrefixes("/permitAll", "/denyAll");
		}

		@Bean
		public MyController myController() {
			return new MyController();
		}
	}

	@Configuration
	static class SyncExecutorConfig {
		@Bean
		public static SyncExecutorSubscribableChannelPostProcessor postProcessor() {
			return new SyncExecutorSubscribableChannelPostProcessor();
		}
	}
}
