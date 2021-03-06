# Proof of Concept to demonstrate Distributed Tracing using Spring Cloud Sleuth
This project has been created to demonstrate the following goals using <a href="https://github.com/spring-cloud/spring-cloud-sleuth">Spring Cloud Sleuth</a>: 
 <ul>
 <li>Trace the execution of a request as it traverses multiple applications</li>
 <li>Demonstrate tracing regardless of the remoting technology used: synchronous Restful calls, asynchronous Restful calls, messaging</li>
 <li>Trace custom request details</li> 
 </ul>   

Before going any further I think it is worth describing what we need to achieve and how Sleuth works.
<p>
<b>Requirements -</b> 
Let's say we have a Restful application which exposes an operation mapped to the route <code>/open</code>. As soon as the application receives a request, we want to create a unique identifier and once the request is handled we want to log the name of the request, its unique identifier, when the request was invoked and when it was processed so that we can measure the time spent. To make things more interesting, our request makes a call to another Restful service, eg. <code>/doAnotherThing</code>. We also want that Restful service to log the request's name, the time spent processing it and more importantly, we want it to use the same unique identifier created by the outer Restful service so that we can correlate them later on.
<br>This could be the logging we would expect based on the requirements: 
<code><pre>
[outerRestApp.log] ["requestUri":"/open", "begin":1453233385739, "end": 1453233386017]
[innerRestApp.log] ["requestUri":"/doAnotherThing", "begin":1453233385740, "end": 1453233386058]
</pre></code>  
<p>
<b>How Sleuth works -</b> 
Upon the arrival of HTTP request and before the corresponding <code>@RequestMapping</code> method is invoked, Sleuth creates a unique identifier called <b>Trace-id</b>. This is true provided the incoming request does not carry a HTTP header called <b>X-Trace-id</b>. Typically, the first request won't have that header therefore Sleuth creates a brand new <b>Trace-id</b>. When the <code>@RequestMapping</code> method returns, Sleuth produces a tracing event. This event essentially contains the name of the request, the unique identifier, the time when it started and ended along with further details. 
Sleuth can collect that event in the standard output, or in a Zipkin server or in a queue/topic. By default, Sleuth will collect the event in the standard output. 
<p>If our Restful application made a downstream HTTP request, Sleuth would intercept it and add the unique identifier to the <b>X-Trace-Id</b> HTTP request's  header. When the downstream Restful application receives the request, Sleuth (assuming the downstream application uses Sleuth too) intercepts the HTTP request, extracts the unique identifier from the <b>X-Trace-Id</b> HTTP header and binds it to the current thread. Once the trace-id is bound to a thread, it can be logged or propagated to another downstream Restful service.
   

<h3>About this project</h3> 
This project consists of 3 standalone applications: A <b>gateway</b> application which acts as a facade and two internal applications, <b>marketgw</b> and <b>portfoliomgr</b>. The gateway exposes a Restful endpoint "/open" which delegates to another two Restful endpoints exposed by the two internal apps, "/openTrade" and "/openPosition" respectively. 

As explained above, Sleuth generates tracing events which can be collected in many ways. One way is by logging them to the standard output. Another way is to send them to a Zipkin server or to a queue/topic. This project will collect them in standard output using Json format. To know how to configure zipkin or messaging check out the <a href="https://github.com/spring-cloud/spring-cloud-sleuth">official documentation</a>. 


<h3>Goal: Demonstrate how we can trace the execution of a request as it traverses several applications using synchronous invocations to external Restful services</h3>
<pre>
----http://localhost:8008/open---> [ Gateway app : SynchronousController class ] 
                                     -------http://localhost:8081/openTrade----> [market-gw app : MarketController class]
                                     <-----Trade---------------------------
                                     ...
                                     apply spread to Trade and produce a DealDone
                                     ... 
                                     ...  // send DealDone to portfoliomgr           										
                                     -------http://localhost:8002/openPosition----> [portfoliomgr app : PortfolioController class]
                                     <-----Position---------------------------
<-----Trade---------------------------										  
  </pre>
   
Request:<p>
<code> 
   curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X POST -d '{"account":"bob","amount":100,"symbol":"EUR/USD"}' http://localhost:8080/open
</code>
<p>
Response:<p>
<pre>
<code> 
HTTP/1.1 200 OK
Server: Apache-Coyote/1.1
X-Trace-Id: 945a8afc-6cc5-4c3a-abfb-cd77c2c4b079
X-Span-Id: 945a8afc-6cc5-4c3a-abfb-cd77c2c4b079
X-Application-Context: bootstrap
Content-Type: application/json;charset=UTF-8
Transfer-Encoding: chunked
Date: Tue, 19 Jan 2016 19:24:13 GMT

{"id":1250656390455939089,"account":"bob","symbol":"EUR/USD","rate":0.01058461760416928,"amount":100.0}
</code>
</pre>

<p>Let's see the tracing generated by Sleuth. For each incoming request, Sleuth creates a unique identifier and uses it to track the request as it traverses multiple applications. This identifier is called Trace-Id. Additionally, Sleuth allows us to attach request's details to the tracing. These details can be:
<ul>
<li>Tag or annotations, e.g. the account who submitted the request: "account":"bob"
<pre><code>
	span.addAnnotation("account", request.account);		
</code></pre>
We can see this annotation in the generated tracing under the attribute "annotations". See below.
<p>  
</li>
<li>Timestamps, e.g. Say we want to capture the time when we calculated the spread and before we send the position to the PositionManager.
<pre><code>
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
</code></pre>
We can see these timestamps in the generated tracing under the attribute "timelineAnnotations". See below.
</li>   
</ul>   

Distributed tracing captured in the standard output in the gateway app:<p>
<pre><code>
[span]{"begin":1453233385739,"end":1453233386017,
<b>"name"</b>:"http/open",
<b>"traceId"</b>:"8cde3cb5-937a-4ee7-bc70-f8dc3b14ab8c","parents":[],
"spanId":"8cde3cb5-937a-4ee7-bc70-f8dc3b14ab8c","remote":false,"exportable":false,
<b>"annotations":{"account":"bob"}</b>,"processId":null,
<b>"timelineAnnotations":[{"time":1453233385751,"msg":"AppliedSpread"}]</b>,"running":false,"accumulatedMillis":278}
[endspan]
</code></pre>
 
Distributed tracing captured in the standard output in the market-gw app:<p>
<pre><code>
[span]{"begin":1453233385745,"end":1453233385750,
"name":"http/openTrade",
"traceId":<b>"8cde3cb5-937a-4ee7-bc70-f8dc3b14ab8c"</b>,
<b>"parents":["8cde3cb5-937a-4ee7-bc70-f8dc3b14ab8c"],</b>
"spanId":"2731612c-9b22-4320-8292-a7cbf82f3e1e","remote":false,"exportable":true,
"annotations":{"/http/request/uri":"http://localhost:8081/openTrade","/http/request/endpoint":"/openTrade","/http/request/method":"POST","/http/request/headers/accept":"application/json, application/*+json","/http/request/headers/content-type":"application/json;charset=UTF-8","/http/request/headers/x-trace-id":"8cde3cb5-937a-4ee7-bc70-f8dc3b14ab8c","/http/request/headers/x-span-id":"8cde3cb5-937a-4ee7-bc70-f8dc3b14ab8c","/http/request/headers/x-span-name":"http/open","/http/request/headers/user-agent":"Java/1.8.0_45","/http/request/headers/host":"localhost:8081","/http/request/headers/connection":"keep-alive","/http/request/headers/content-length":"32","mkt":"6915959203315918557","/http/response/status_code":"200","/http/response/headers/x-trace-id":"8cde3cb5-937a-4ee7-bc70-f8dc3b14ab8c","/http/response/headers/x-span-id":"2731612c-9b22-4320-8292-a7cbf82f3e1e","/http/response/headers/x-application-context":"bootstrap:8081","/http/response/headers/content-type":"application/json;charset=UTF-8","/http/response/headers/transfer-encoding":"chunked","/http/response/headers/date":"Tue, 19 Jan 2016 19:56:25 GMT"},"processId":null,
"timelineAnnotations":[],"running":false,"accumulatedMillis":5}
[endspan]
</code></pre>
 

Sleuth is responsible of carrying the trace-Id across remote Restful calls. 

<h3>Goal: Demonstrate how we can trace the execution of a request as it traverses several applications using asynchronous invocations to external Restful services</h3>
The Use case is practically same as the synchronous one with the exception that this time the 2 internal Restful requests are done asynchronously. Furthermore, the @RequestMapping method that attends the request in the Gateway is also asynchronous, i.e. it does not return a result but a <code>ListenableFuture<Position></code>. 

What has this anything to do with Sleuth? Sleuth has to keep sending the trace-id to downstream services. And in addition to that, the thread that created the Trace-id is gone! this was the servlet's thread that handled the initial request. The responses from the internal restful request are now handled by a different thread. Sleuth has to be able to propagate the trace-id from the http response to the thread handling the response.

<b>This is a piece of functionality not <a href="https://github.com/spring-cloud/spring-cloud-sleuth/issues/124">supported by Spring yet</a> </b>
 
 <pre>
----http://localhost:8008/async/open---> [ Gateway app : AsyncController class ] 
                                     -------http://localhost:8081/openTrade----> [market-gw app : MarketController class]
                                     {Nio thread}<-----Trade---------------------------
                                            ...
                                            apply spread to Trade and produce a DealDone
                                            ... 
                                            ...  // send DealDone to portfoliomgr           										
                                            -------http://localhost:8002/openPosition----> [portfoliomgr app : PortfolioController class]
                                            <-----Position---------------------------
<-----Trade----------------------------------										  
  </pre>


<h3>Goal: Demonstrate distributed tracing when applications collaborate via messaging</h3>
This time the Gateway application sends a message/request to one of the internal applications. Sleuth has to be able to propagate the trace-id over to the receiver of the message/request. 
 
Pending to do

<h3>Goal: Demonstrate distributed tracing when applications collaborate via Hystrix+Ribbon</h3>
This time the Gateway application calls downstream applications thru Ribbon and Hystrix. Sleuth has to be able to propagate the trace-id.
 
Pending to do


<h3>Issues to be solved</h3>
<ul>
<li>how do I configure Sleuth not to generate a span event for unmapped routes? It seems it is tracking every incoming requests, eg. /somecrazyrequest will produce a 
span event. There is a TraceFilter used to filter some mappings like /info and others. But this is not enough. </li>
</ul>


