package io.pivotal.demo.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.AsyncRestTemplate;

@Configuration
public class GatewayConfiguration {

	@Bean
    public AsyncRestTemplate asyncRestTemplate() {
		// TODO create it with a custom thread-pool
		return new AsyncRestTemplate(new HttpComponentsAsyncClientHttpRequestFactory());
	} 
}
