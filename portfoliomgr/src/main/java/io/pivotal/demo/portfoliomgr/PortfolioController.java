package io.pivotal.demo.portfoliomgr;

import java.util.Date;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortfolioController {
	
	@Autowired
	private TraceManager traceManager;
	

	@RequestMapping(value = "openPosition", method = RequestMethod.POST)
	public Position open(@RequestBody DealDone deal) {
		traceManager.addAnnotation("account", deal.account);
		Random random = new Random();
		
		return new Position(Math.abs(random.nextLong()), deal.account, deal.symbol, deal.price, deal.amount);
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
		
		public Position(long id, String account, String symbol, double rate, double amount) {
			super();
			this.id = id;
			this.account = account;
			this.symbol = symbol;
			this.rate = rate;
			this.amount = amount;
		}	
		public Position() {
			
		}
		
	}
	
}
