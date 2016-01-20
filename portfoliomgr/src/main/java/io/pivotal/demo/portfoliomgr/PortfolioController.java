package io.pivotal.demo.portfoliomgr;

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
	public Trade open(@RequestBody Trade request) {
		traceManager.addAnnotation("account", request.account);
		return request;
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
}
