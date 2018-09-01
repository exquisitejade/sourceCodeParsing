/*
 * Copyright 2013-2018 the original author or authors.
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
 *
 */

package org.springframework.cloud.gateway.test.ssl;

import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.netty.http.client.HttpClient;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.test.BaseWebClientTests;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@DirtiesContext
@ActiveProfiles("ssl")
public class SSLTests extends BaseWebClientTests {

	@Before
	public void setup() {
		try {
			SslContext sslContext = SslContextBuilder.forClient()
					.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
			HttpClient httpClient = HttpClient.create().secure(ssl -> {
				ssl.sslContext(sslContext);
			});
			ClientHttpConnector httpConnector = new ReactorClientHttpConnector(
					httpClient);
			baseUri = "https://localhost:" + port;
			this.webClient = WebClient.builder().clientConnector(httpConnector)
					.baseUrl(baseUri).build();
		}
		catch (SSLException e) {
			throw new RuntimeException(e);
		}		
	}
	
	@Test
	public void testSslTrust() {
		ClientResponse clientResponse = webClient.get().uri("/ssltrust")
				.exchange().block();
		HttpStatus statusCode = clientResponse.statusCode();
		assertTrue(statusCode.is2xxSuccessful());
				
	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	@Import(DefaultTestConfig.class)
	@RestController
	public static class TestConfig {

		@RequestMapping("/httpbin/ssltrust")
		public ResponseEntity<Void> nocontenttype() {
			return ResponseEntity.status(204).build();
		}

	}

}
