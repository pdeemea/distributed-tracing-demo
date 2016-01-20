# Proof of Concept to demonstrate Distributed Tracing 
This project has been created to demonstrate how we can use Spring Cloud Sleuth to implement distributed tracing in a distributed architecture composed of several applications that interact each other. There are 3 goals we want to achieve:
 <ul>
 <li>Show we can trace the execution of a request as it traverses multiple applications</li>
 <li>Show various mechanisms used by application to collaborate each other: synchronous calls, asynchronous calls, messaging</li>
 <li>Show how we can capture request details</li> 
 </ul>   

This project consists of 3 standalone applications: A <b>gateway</b> application which acts as a facade and two internal applications, <b>marketgw</b> and <b>portfoliomgr</b>. The gateway exposes a Restful endpoint "/open" which delegates to another two Restful endpoints exposed by the two internal apps.

     
<h3>Goal: Demonstrate how we can trace the execution of a request as it traverses several applications using synchronous invocations to external Restful services</h3>
Gateway application receives the request below. It calls synchronously the marketgw Restful application. When it gets the result, it uses to call another Restful application, the portfoliomgr. When the gateway gets the result from the portfoliomgr, it sends it back as a response.
 
Request:<p>
<code> 
   curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X POST -d '{"account":"bob","amount":100}' http://localhost:8080/open
</code>
<p>
Response:<p>
<code>
<pre> 
HTTP/1.1 200 OK
Server: Apache-Coyote/1.1
X-Trace-Id: 945a8afc-6cc5-4c3a-abfb-cd77c2c4b079
X-Span-Id: 945a8afc-6cc5-4c3a-abfb-cd77c2c4b079
X-Application-Context: bootstrap
Content-Type: application/json;charset=UTF-8
Transfer-Encoding: chunked
Date: Tue, 19 Jan 2016 19:24:13 GMT

{"id":"916d50c0-01b3-436a-93aa-7c76a5692d7d","account":"bob","rate":0.5494186987570098,"amount":100.0}
</pre>
</code>

<p>Sleuth creates a Trace-Id that will use for as long as the request last. It will be the distributed transaction reference used across all the services invoked as part of each request. For each request's handler, Sleuth creates another identifier called Span-id. For instance, every @RequestMapping has its own span-id.

Sleuth is responsible of carrying the trace-Id across remote Restful calls. 

<h3>Goal: Demonstrate how we can trace the execution of a request as it traverses several applications using asynchronous invocations to external Restful services</h3>
The Use case is practically same as the synchronous one with the exception that this time the 2 internal Restful requests are done asynchronously. What has this anything to do with Sleuth? The thread that created the Trace-id is gone! this was the servlet's thread that handled the initial request. The responses from the internal restful request are now handled by a different thread. Sleuth has to be able to propagate the trace-id from the http response to the thread handling the response.

Work in progress

<h3>Use Case - Messaging collaboration</h3>
This time the Gateway application sends a message/request to one of the internal applications. Sleuth has to be able to propagate the trace-id over to the receiver of the message/request. 
 
Pending to do

<h3>Issues to be solved</h3>
<ul>
<li>how do I configure the requests I want Sleuth to track? It seems it is tracking every incoming requests, eg. /somecrazyrequest will produce a 
span event</li>
</ul>


