package io.pivotal.demo.marketgw;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MarketController {
	
	@Autowired
	private TraceManager traceManager;
	
	@RequestMapping(value = "openTrade", method = RequestMethod.POST)
	public Trade open(@RequestBody MktTradeRequest request) {
		Random random = new Random();
		
		traceManager.addAnnotation("mkt", String.valueOf(random.nextLong()));
		return new Trade(UUID.randomUUID().toString(), request.symbol, "LP" + random.nextInt(), random.nextDouble(), request.amount);
	}
	

	public static class Trade {
		public String id;
		public String symbol;
		public Date tradeDt;
		public String lp;
		public double rate;
		public double amount;
		
		public Trade(String id, String symbol, String lp, double rate, double amount) {
			super();
			this.id = id;
			this.symbol = symbol;
			this.lp = lp;
			this.rate = rate;
			this.amount = amount;
			this.tradeDt = new Date();
		}	
		public Trade() {
			
		}
		
	}
	public static class MktTradeRequest {
		public String symbol;
		public double amount;
		
	}
	
}
