package io.pivotal.demo.gateway;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GatewayConfiguration {

	// @Bean
	// public AsyncRestTemplate asyncRestTemplate() {
	// // TODO create it with a custom thread-pool
	// return new AsyncRestTemplate(new
	// HttpComponentsAsyncClientHttpRequestFactory());
	// }

	@Bean
	AsyncRestTemplate asyncRestTemplate(@LoadBalanced RestTemplate restTemplate) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setTaskExecutor(threadPoolTaskScheduler());
		return new AsyncRestTemplate(requestFactory, restTemplate);
	}

	@Bean(destroyMethod = "shutdown")
	ThreadPoolTaskScheduler threadPoolTaskScheduler() {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.initialize();
		return threadPoolTaskScheduler;
	}
}
