package io.pivotal.demo.gateway;

import java.util.Date;
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
public class SynchronousController  {

	private static Logger log = LoggerFactory.getLogger(SynchronousController.class);
	
	@Autowired
	private TraceManager traceManager;
	
	@Autowired 
	private RestTemplate restTemplate;
	
	
	@Value("${marketgw}") 
	private String marketgw;
	
	@Value("${portfoliomgr}") 
	private String portfoliomgr;
	
	
	@RequestMapping(value = "open", method = RequestMethod.POST)
	public Position open(@RequestBody TradeRequest request) throws InterruptedException, ExecutionException {
		Span span = traceManager.getCurrentSpan();

		log.info("Opening trade {} {} @ {}", request.account, request.amount, span.getTraceId());
		
		// Trace statements must carry the account involved in the request
		// TODO find a less intrusive code. For instance, thru annotation, aspects, others.
		span.addAnnotation("account", request.account);
		
		// First, execute the trade in the market by calling market Gateway rest service.  
		Trade trade = restTemplate.postForObject(marketgw + "/openTrade", new MktTradeRequest(request), Trade.class);
		
		log.info("Opened trade {}", trade);
		
		// once we receive the trade from the market, we calculate some complex value 
		DealDone deal = applySpread(request, trade);
		
		return restTemplate.postForObject(portfoliomgr + "/openPosition", deal, Position.class);
			
	}
	

	/**
	 * Calculate other complex value. Track the time spent.  
	 * @throws InterruptedException 
	 */
	public DealDone applySpread(TradeRequest request, Trade trade) throws InterruptedException {
	
		Span span = traceManager.getCurrentSpan();
		
		// do something
		try {
			Thread.sleep(250);
			double price = trade.rate + 0.01; // very simplistic spread. 
			return new DealDone(trade.id, trade.symbol, request.account, price, trade.amount);
		} finally {
			span.addTimelineAnnotation("AppliedSpread");
		}
		
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
		@Override
		public String toString() {
			return "Trade [id=" + id + ", symbol=" + symbol + ", tradeDt=" + tradeDt + ", lp=" + lp + ", rate=" + rate
					+ ", amount=" + amount + "]";
		}
		
	}
	public static class DealDone {
		public String lpRef;
		public String symbol;
		public Date tradeDt;
		public double price;
		public double amount;
		public String account;
		
		public DealDone(String lpRef, String symbol, String account, double price, double amount) {
			super();
			this.lpRef = lpRef;
			this.symbol = symbol;
			this.price = price;
			this.amount = amount;
			this.account = account;
		}	
		public DealDone() {
			
		}
		
	}
	
	
	
	
	public static class Position {
		public long id;
		public String account;
		public String symbol;
		public double rate;
		public double amount;
		
			
		public Position() {
			
		}
		
	}
	
	public static class TradeRequest {
		public String account;
		public String symbol;
		public double amount;
		
	}
	public static class MktTradeRequest {
		public String symbol;
		public double amount;
		
		public MktTradeRequest() {
			
		}
		public MktTradeRequest(TradeRequest request) {
			this.symbol = request.symbol;
			this.amount = request.amount;
		}
		
	}
	
	
	
}

