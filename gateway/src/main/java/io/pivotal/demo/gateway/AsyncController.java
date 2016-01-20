package io.pivotal.demo.gateway;

import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/async")
public class AsyncController  {

	private static Logger log = LoggerFactory.getLogger(AsyncController.class);
	
	@Autowired
	private TraceManager traceManager;
	
	@Autowired
	private AsyncRestTemplate asyncRestTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Value("${marketgw}") 
	private String marketgw;
	
	@Value("${portfoliomgr}") 
	private String portfoliomgr;
	
	
	@RequestMapping(value = "open", method = RequestMethod.POST)
	public ListenableFuture<ResponseEntity<SynchronousController.Position>> open(@RequestBody SynchronousController.TradeRequest request) throws InterruptedException, ExecutionException {
		Span span = traceManager.getCurrentSpan();

		log.info("Opening trade {} {} @ {}", request.account, request.amount, span.getTraceId());
		
		span.addAnnotation("account", request.account);
		
		// First async rest call: Execute the trade by calling market Gateway Restful service.  
		ListenableFuture<ResponseEntity<SynchronousController.Trade>> openedMktTrade = this.asyncRestTemplate.postForEntity(
				marketgw + "/openTrade", 
				new HttpEntity<SynchronousController.MktTradeRequest>(new SynchronousController.MktTradeRequest(request)),
				SynchronousController.Trade.class);

//		openedMktTrade.addCallback(t -> {
//			
//			calculateSomeOtherComplexValue(t.getBody());
//			
//			restTemplate.postForObject(portfoliomgr + "/openPosition", trade, Trade.class);
//			
//			
//		}, e -> { } );
		// TODO when we receive an openedMktTrade we call calculateSomeOehterComplexValue and then we asynchronously call
		// the Restful service positionMgr.
		
		return null; 		// TODO return the ListenableFuture from the 2nd async rest call
		
	}
	

	
}

