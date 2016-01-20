package io.pivotal.demo.marketgw;

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
	public Trade open(@RequestBody TradeRequest request) {
		Random random = new Random();
		
		traceManager.addAnnotation("mkt", String.valueOf(random.nextLong()));
		return new Trade(UUID.randomUUID().toString(), request.account, random.nextDouble(), request.amount);
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
