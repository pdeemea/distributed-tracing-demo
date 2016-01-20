package io.pivotal.demo.gateway;

import java.util.concurrent.CompletableFuture;
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
import org.springframework.web.context.request.async.DeferredResult;

import io.pivotal.demo.gateway.SynchronousController.DealDone;
import io.pivotal.demo.gateway.SynchronousController.Position;
import io.pivotal.demo.gateway.SynchronousController.Trade;
import io.pivotal.demo.gateway.SynchronousController.TradeRequest;


@RestController
@RequestMapping("/async")
public class AsyncController {

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
	public DeferredResult<SynchronousController.Position> open(
			@RequestBody SynchronousController.TradeRequest request) throws InterruptedException, ExecutionException {
		Span span = traceManager.getCurrentSpan();

		log.info("Opening trade {} {} @ {}", request.account, request.amount, span.getTraceId());

		span.addAnnotation("account", request.account);

		DeferredResult<SynchronousController.Position> deferredResult = new DeferredResult<>();

		// First async rest call: Execute the trade by calling market Gateway
		// Restful service.ç
		
		ListenableFuture<ResponseEntity<SynchronousController.Trade>> openedMktTrade = this.asyncRestTemplate
				.postForEntity(marketgw + "/openTrade", new HttpEntity<SynchronousController.MktTradeRequest>(
						new SynchronousController.MktTradeRequest(request)), SynchronousController.Trade.class);

		openedMktTrade.addCallback((ResponseEntity<SynchronousController.Trade> t) -> {
			
			DealDone deal = applySpread(request, t.getBody());
			
			Position position = restTemplate.postForObject(portfoliomgr + "/openPosition", deal, Position.class);
			
            deferredResult.setResult(position);
                
		}, e -> {
			deferredResult.setErrorResult(e);
		}
        );
		
		return deferredResult; 

	}

   	/**
	 * Calculate other complex value. Track the time spent.  
	 * @throws InterruptedException 
	 */
	public DealDone applySpread(TradeRequest request, Trade trade) {
	
		Span span = traceManager.getCurrentSpan();
		
		// do something
		try {
			Thread.sleep(250);
			double price = trade.rate + 0.01; // very simplistic spread. 
			return new DealDone(trade.id, trade.symbol, request.account, price, trade.amount);
		}catch(InterruptedException e) {
			return null;
		}finally {
			if (span !=  null) {
				span.addTimelineAnnotation("AppliedSpread");
			}else {
				log.warn("No Span avaliable in applySpread method");
			}
		}
		
	}

}
