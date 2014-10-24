rest-client-tools
=======================

Library for generating REST client proxies. The library is primarily designed for consuming JAX-RS resources. However, you can also
generate client proxies for any http endpoint that you can describe using JAX-RS annotations. The code borrows heavily from RESTEasy 2.3.4.Final.

######Motivation

The [RestEasy client framework](http://docs.jboss.org/resteasy/docs/1.0.1.GA/userguide/html/RESTEasy_Client_Framework.html#Sharing_interfaces) is an easy to use
way to generate client proxies for consuming REST services. However, the RestEasy framework cannot be used out of the box in the same JVM where another JAX-RS 
implementation such as Jersey is already on the classpath. This can even be a problem when 2 different versions of the RestEasy jars are on the classpath.
The heart of the matter lies in how JAX-RS initializes a javax.ws.rs.ext.RuntimeDelegate [instance][1].
Only one RuntimeDelegate is loaded per JVM and either Jersey or RestEasy wins. Then when your RestEasy client proxy tries to use the Jersey RuntimeDelegate you get Exceptions
like the following:

    java.lang.ClassCastException: com.sun.jersey.server.impl.provider.RuntimeDelegateImpl cannot be cast to org.jboss.resteasy.spi.ResteasyProviderFactory

To eliminate these conflicts and to make the client framework more portable we did a few important things:

1. Extracted just the proxy generation code from RestEasy 
2. Judicious refactoring so that we no longer use the RuntimeDelegate when creating client proxies. 
3. Streamlined the remaining code as much as possible and reworked the API to have a friendlier fluent builder interface

Client proxies created with rest-client-tools are able to run alongside any JAX-RS implementation without conflict. This allows for client to be used
in a much broader set of scenarios. For example, if a team has a service in production written using Jersey and has the need to 
consume an external REST service they can use rest-client-tools without conflict and without having to fully switch to RestEasy. 
rest-client-tools is designed and tested to be interoperable with any JAX-RS implementation that you choose. 

Additionally, there are other situations where you may need to consume some http endpoint but don't want to put a full blown JAX-RS 
implementation on your classpath. In that case just:
 
 - include the JAX-RS api jar (so that the JAX-RS annotations are available)
 - configure JAX-RS to use the com.opower.rest.client.generator.core.BasicRuntimeDelegate (see the javadoc for details) 

Now you can create client proxies in apps where you don't want or need any server side component of JAX-RS.


######Features

  * Specify your own MessageBodyReader / Writer to have control over the message body serialization.
  * ClientRequestFilters allow you to alter the HTTP request before it is send (adding headers etc.)
  * ClientErrorInterceptors allow for custom handling of failed http service calls.
  * Automatic Hystrix circuit breaker integration when using the HystrixClient.Builder
  
######API example

Here is an example of a resource interface. Note that it's simply a java interface annotated with JAX-RS annotation.
 We recommend using this interface for both clients and server side resource implementations. That keeps the service interface
 consistent. 

    @Path("/frob")
    @Produces(MediaType.APPLICATION_JSON)
    public interface FrobResource {
        @POST
        @Path("{frobId}")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        Frob updateFrob(@PathParam("frobId")String frobId, @FormParam("name") String name);
   
        @GET
        @Path("{frobId}")
        Frob findFrob(@PathParam("frobId") String frobId);
   
        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        Response createFrob(Frob frob);
    }
 
 Here is a server side resource implementation example:
 
    public class FrobServerResource implments FrobResource {
      // notice there are no JAX-RX annotations here. That is because
      // the annotations belong on the interface ONLY. In some cases this
      // can cause your endpoints to not function correctly. Or if you only put
      // an annotation here and its missing from the interface, your clients won't
      // function correctly since they don't have access to your server side resources
      public Frob updateFrob(String frobId, String name) {
          // do work here probably with dao and stuff
      }
       
      public Frob findFrob(String frobId) {
          // find frob work here
      }
   
      public Response createFrob(Frob frob) {
          // do more work
      }
  }
   
 
  Once you have imported the Client.Builder you can use its fluent interface to create and customize a proxy instance for a given resource interface. 
  
  Here are a few of the features of the new proxy client:
  They are thread-safe and intended to be singletons.
  They wrap calls in HystrixCommands to gives you circuit breaker protection.
  The rest-client-tools proxy builder requires a UriProvider implementation that tells clients how to find a service instance.
  
    Client.Builder<FrobResource> clientBuilder = new Client.Builder<>(FrobResource.class, serviceDiscovery, serviceName, OAUTH_CLIENT_ID)
                              .clientSecret(OAUTH_CLIENT_SECRET) // by specifying the oauth2 client secret, 
                                                                 // you enable the auth-service integration
                              
  You may need to alter the proxy's requests before they are sent. For instance, you may need to add a header or some other parameter to the request.
  
    ClientRequestFilter filter = new ClientRequestFilter() {
              @Override
              public void filter(ClientRequest request) {
                  // add a http header to the client requests
                  request.header("header name", "header value");
              }
          };
   
    clientBuilder.addClientRequestFilter(filter); // filters are applied in the order they are added.
  
  ClientErrorInterceptor defines the proxy's behavior in case of errors. Here is how you would specify your own list of custom ClientErrorInterceptors.
  
    List<ClientErrorInterceptor> interceptors = ImmutableList.<ClientErrorInterceptor>of(new ClientErrorInterceptor() {
              @Override
              public void handle(ClientResponse<?> response) throws RuntimeException {
                  // handle the error response as you see fit
                  throw new SpecialException(response.getResponseStatus());
              }
          });
   
    clientBuilder.clientErrorInterceptors(interceptors); 
    
  If your service produces and consumes JSON it is easy to use the JacksonJsonProvider for serialization.
  
    JacksonJsonProvider jsonProvider = new JacksonJsonProvider();
     
    clientBuilder.messageBodyProviders(jsonProvider, jsonProvider);
 
 
  Client proxy instances require a ClientExecutor instance that will actually perform the http requests.
  
    ClientExecutor executor = new ApacheHttpClient4Executor();
    clientBuilder.executor(executor);
    
    
  If you use the HystrixClient.Builder, then all method invocations on client proxies are wrapped with a HystrixCommand object. 
  Each method on your resource interface will receive its own HystrixCommandKey. 
  This key can be overridden but shouldn't need to be.
  The HystrixCommandProperties and HystrixThreadPoolProperties can be configured for individual methods or for the whole resource. 
  You can also specify a fallback if a command fails. Have a look:
   
    Method findFrob = FrobResource.class.getMethod("findFrob");
    clientBuilder.methodCommandKey(findFrob, HystrixCommandKey.Factory.asKey("BEAUTIFUL_SNOWFLAKE"));
   
    // this changes the command timeout for just the findFrob method with razor-like precision
    clientBuilder.methodProperties(findFrob, new ConfigurationCallback<HystrixCommandProperties.Setter>() {
                      @Override
                      public void configure(HystrixCommandProperties.Setter setter) {
                          setter.withExecutionIsolationThreadTimeoutInMilliseconds(5000);
                      }
                  });
   
    // you don't have to do it for each method invidually though. 
    // Let's change the core pool size for all of the methods on the ResourceInterface
    clientBuilder.threadPoolProperties(new ConfigurationCallback<HystrixThreadPoolProperties.Setter>() {
                      @Override
                      public void configure(HystrixThreadPoolProperties.Setter setter) {
                          setter.withCoreSize(10);
                      }
                  });
   
    // how about a fallback?
    clientBuilder.methodFallback(findFrob, new Callable<Object>() {
              @Override
              public Object call() throws Exception {
                  Frob default = new Frob();
                  default.setId("default");
                  default.setName("Generic");
                  return default;
              }
          });

[1]: http://docs.oracle.com/javaee/6/api/javax/ws/rs/ext/RuntimeDelegate.html#getInstance()
