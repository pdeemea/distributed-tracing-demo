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
	public ListenableFuture<ResponseEntity<Position>> open(@RequestBody TradeRequest request) throws InterruptedException, ExecutionException {
		Span span = traceManager.getCurrentSpan();

		log.info("Opening trade {} {} @ {}", request.account, request.amount, span.getTraceId());
		
		span.addAnnotation("account", request.account);
		
		// First async rest call: Execute the trade by calling market Gateway Restful service.  
		ListenableFuture<ResponseEntity<Trade>> openedMktTrade = this.asyncRestTemplate.postForEntity(marketgw + "/openTrade", 
				new HttpEntity<TradeRequest>(request), Trade.class);

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
	

	/**
	 * Calculate other complex value. Track the time spent.  
	 */
	public void calculateSomeOtherComplexValue(Trade trade) {
	
		Span span = traceManager.getCurrentSpan();
		
		span.addTimelineAnnotation("StartCalculation");
		// do something
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		span.addTimelineAnnotation("EndCalculation");
		
	}
	
	public static class Trade {
		public String id;
		public String symbol;
		public Date tradeDt;
		public String lp;
		public double rate;
		public double amount;
		
		public Trade(String id, String symbol, double rate, double amount) {
			super();
			this.id = id;
			this.symbol = symbol;
			this.rate = rate;
			this.amount = amount;
		}	
		public Trade() {
			
		}
		
	}
	
	public static class Position {
		public String id;
		public String account;
		public double rate;
		public double amount;
		
		public Position(String id, String account, double rate, double amount) {
			super();
			this.id = id;
			this.account = account;
			this.rate = rate;
			this.amount = amount;
		}	
		public Position() {
			
		}
		
	}
	
	public static class TradeRequest {
		public String account;
		public double amount;
	}
	
	
}

