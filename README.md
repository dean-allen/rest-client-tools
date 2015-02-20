opower-rest-client-tools
=================

[![Build Status](http://jenkins-dev.va.opower.it/job/opower-rest-client-tools/badge/icon)](http://jenkins-dev.va.opower.it/job/opower-rest-client-tools/)

Portions of rest-client-tools have been open sourced -- checkout the README [here](https://github.com/opower/rest-client-tools)

This repository contains only the Opower specific extensions to rest-client-tools. 

- integration with authorization-service
- CuratorUriProvider for using our zookeeper service registration setup
- Automatic metrics publishing to opentsdb
- JSON serialization pre configured with sane defaults
- Default exception handling preconfigured

More information can be found [here](https://wiki.opower.com/display/PD/Archmage+Client+Migration+Guide)


@ResourceMetadata
-----------------
As of 1.1.0 all resource interfaces should be annotated with @ResourceMetadata. In archmage services, this is the interface in
your <service>-interface project. This is an important change that makes consuming services even easier as now clients won't
have to figure out which serviceName to use and won't have to worry about squelching warnings from resource interface validation.
We have deprecated all the old constructors in OpowerClient.Builder that required you to pass in a serviceName. Now service writers
should provide the serviceName via the @ResourceMetadata annotation on their resource interface. Here is an example:


    @ResourceMetadata(serviceName = "example", serviceVersion = 1, modelPackages = { "com.opower.example.model" })
    @Path("/foo")
    @Produces(MediaType.APPLICATION_JSON)
    public interface FooResource {
        @GET
        String getFoo();
    }

Note the modelPackages field is optional.

UriProvider
-----------
To construct a client proxy you must provide a UriProvider. The UriProvider is responsible for providing the base url for each
client request. In most cases using the SimpleUriProvider will be sufficient. In the case of archmage services, the SimpleUriProvider
in conjunction with synapse-lite is the official way to configure your clients. Here is an example of how this is done:

    // this is prod, we also have dev and stage synapse environments
    UriProvider uriProvider = new SimpleUriProvider("http://prod-synapse.va.opower.it:8999");

    OpowerClient.Builder<FrobResource> clientBuilder = new OpowerClient.Builder<>(FrobResource.class, uriProvider, "clientId");

There are 3 synapse-lite environments. Use these urls in the constructor of SimpleUriProvider as shown above:

 * http://dev-synapse.va.opower.it:8999
 * http://stage-synapse.va.opower.it:8999
 * http://prod-synapse.va.opower.it:8999

Configuration Overview
======================

There are many different options for configuring your client instances. What follows is an overview of the various settings that are available to you. We have provided sensible defaults that should work in many cases. However you might have special settings needed for your particular client. Note that these builder methods should be invoked before calling build().

The client's authorization-service integration automatically refreshes tokens for you. This process will start before the token expires to provide a buffer against failed requests. This setting specifies the number of seconds prior to token expiration that the refresh process should start. The default value is 2 seconds. If you need to expand this buffer you can do so like this:

    clientBuilder.tokenTtlRefreshSeconds(5); // isn't that nice that the methods spells out the units?

Metrics collection and publishing to sensu are enabled by default. If for some strange reason you need to disable this (and it would indeed be strange) you can disable one or both. If you feel you need to disable this, please discuss your requirements with Core Platform.

    clientBuilder.disableMetrics(); // sad y u no like metrics?
    clientBuilder.disableSensuPublishing(); // also sad :(

You may need to alter the proxy's requests before they are sent. For instance, you may need to add a header or some other parameter to the request. (By default, your client adds a request_id parameter and an X-Service-Name header.) You use ClientRequestFilter to customize the requests:

    ClientRequestFilter filter = new ClientRequestFilter() {
            @Override
            public void filter(ClientRequest request) {
                // add a http header to the client requests
                request.header("header name", "header value");
            }
    };
 
    clientBuilder.addClientRequestFilter(filter); // filters are applied in the order they are added.
    
ClientErrorInterceptor defines the proxy's behavior in case of errors. By default, each OpowerClient uses the ExceptionMapperInterceptor which maps http status codes to nicely named exceptions. This should be sufficient for most cases. Check out the source code of ExceptionMapperInterceptor if you have more questions. Here is how you would specify your own list of custom ClientErrorInterceptors, which would replace the default ClientErrorInterceptor:

    List<ClientErrorInterceptor> interceptors = ImmutableList.<ClientErrorInterceptor>of(new ClientErrorInterceptor() {
            @Override
            public void handle(ClientResponse<?> response) throws RuntimeException {
                // handle the error response as you see fit
                throw new SpecialException(response.getResponseStatus());
            }
        });
 
    clientBuilder.clientErrorInterceptors(interceptors); // replaces the default list. The default is pretty good are you sure?

If you need to make changes to the Jackson layer – e.g., enabling or disabling features based on the remote service's requirements -- you can use a ConfigurationCallback. The OpowerClient.Builder allows you to customize the internal JacksonJsonProvider / ObjectMapper instances via ConfigurationCallback instances. Think of it as a ruby block or a lambda when we finally get java 8.  Here is how you use ConfigurationCallback:

    clientBuilder.configureObjectMapper(new ConfigurationCallback<ObjectMapper>() {
                            @Override
                            public void configure(ObjectMapper objectMapper) {
                                // here you can configure the ObjectMapper as needed
                                objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
                            }
                        })
      .configureJacksonJsonProvider(new ConfigurationCallback<JacksonJsonProvider>() {
                            @Override
                            public void configure(JacksonJsonProvider jacksonJsonProvider) {
                                // here you can configure the JacksonJsonProvider as needed
                                jacksonJsonProvider.configure(DeserializationFeature.WRAP_EXCEPTIONS, false);
                            }
                        });

Client proxy instances use Apache's httpclient to make requests. By default, requests that cause a 503 response will be retried once after one second. You can tweak the number of retries and starting interval for retries (the interval increases exponentially up to 10s). Also you can manipulate the HttpParams instance for the client using the ConfigurationCallback mechanism.

    clientBuilder.configureRetries(3, 3000) // the sleep interval is in milliseconds
        .httpClientParams(new ConfigurationCallback<HttpParams>() {
              @Override
              public void configure(HttpParams httpParams) {
              // http://hc.apache.org/httpcomponents-client-4.2.x/httpclient/apidocs/org/apache/http/client/params/HttpClientParams.html
                  HttpClientParams.setRedirecting(httpParams, true);               
              }
        });
        
All method invocations on client proxies are wrapped with a HystrixCommand object. Each method on your resource interface will receive its own HystrixCommandKey. This key can be overridden but shouldn't need to be (we know; you're a snowflake). The HystrixCommandProperties and HystrixThreadPoolProperties can be configured for individual methods or for the whole resource. You can also specify a fallback if a command fails. Have a look:
 
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

Use for X-OPOWER-JsonEnvelope Services
---------------------------------------

If you need to use opower-rest-client-tools to interact with bertha, scrapi or any X-OPOWER-JsonEnvelope service you can configure you client like:

    clientBuilder.useJsonEnvelope();

This will set your Jackson processor and error interceptor to versions that expect to unwrap responses and errors. All other configuration
options function normally.

######Releasing new version
1. Run the [Jenkins job](https://jenkins-dev.va.opower.it/job/rest-client-tools-release)
2. Verify the version is [published](https://nexus.va.opower.it/nexus/content/groups/public/com/opower/opower-rest-client-builder)
