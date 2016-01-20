package io.pivotal.demo.gateway;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class VanillController  {

	private static Logger log = LoggerFactory.getLogger(VanillController.class);
	
	@Autowired
	private TraceManager traceManager;
	
	@Autowired 
	private RestTemplate restTemplate;
	
	
	@Value("${marketgw}") 
	private String marketgw;
	
	@Value("${portfoliomgr}") 
	private String portfoliomgr;
	
	
	@RequestMapping(value = "open", method = RequestMethod.POST)
	public Trade open(@RequestBody TradeRequest request) throws InterruptedException, ExecutionException {
		Span span = traceManager.getCurrentSpan();

		log.info("Opening trade {} {} @ {}", request.account, request.amount, span.getTraceId());
		
		// Trace statements must carry the account involved in the request
		// TODO find a less intrusive code. For instance, thru annotation, aspects, others.
		span.addAnnotation("account", request.account);
		
		// First, execute the trade in the market by calling market Gateway rest service.  
		Trade trade = restTemplate.postForObject(marketgw + "/openTrade", request, Trade.class);
		
		// once we receive the trade from the market, we calculate some complex value 
		calculateSomeOtherComplexValue(trade);
		
		return restTemplate.postForObject(portfoliomgr + "/openPosition", trade, Trade.class);
			
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
		public String account;
		public double rate;
		public double amount;
		
		public Trade(String id, String account, double rate, double amount) {
			super();
			this.id = id;
			this.account = account;
			this.rate = rate;
			this.amount = amount;
		}	
		public Trade() {
			
		}
		
	}
	
	public static class TradeRequest {
		public String account;
		public double amount;
	}
	
	
}

