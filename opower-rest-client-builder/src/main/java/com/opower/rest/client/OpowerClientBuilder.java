package com.opower.rest.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.opower.auth.resources.oauth2.AccessTokenResource;
import com.opower.metrics.FactoryLoaders;
import com.opower.metrics.MetricsProvider;
import com.opower.metrics.MetricsProviderFactory;
import com.opower.metrics.SensuConfiguration;
import com.opower.metrics.SensuPublisherFactory;
import com.opower.rest.client.curator.CuratorUriProvider;
import com.opower.rest.client.filters.RequestIdFilter;
import com.opower.rest.client.filters.ServiceNameClientRequestFilter;
import com.opower.rest.client.filters.auth.AuthorizationClientRequestFilter;
import com.opower.rest.client.filters.auth.Oauth2AccessTokenRequester;
import com.opower.rest.client.filters.auth.Oauth2CredentialRequestingClientRequestFilter;
import com.opower.rest.client.generator.core.ClientBuilder;
import com.opower.rest.client.generator.core.ClientErrorInterceptor;
import com.opower.rest.client.generator.core.ClientRequestFilter;
import com.opower.rest.client.generator.core.UriProvider;
import com.opower.rest.client.generator.executors.ApacheHttpClient4Executor;
import com.opower.rest.client.generator.hystrix.HystrixClientBuilder;
import com.opower.rest.client.http.ExponentialRetryStrategy;
import com.opower.rest.client.hystrix.OpowerEventNotifier;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.AutoRetryHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builder class used to create rest client proxies with special care take to provide sensible
 * defaults for the Opower landscape. By default, the client will
 * - use curator to resolve server urls,
 * - collect timing metrics
 * - publish metrics to sensu for reporting in OpenTSDB
 * - execute each method as a customizable HystrixCommand
 * - use Jackson for serialization
 * <p/>
 * There are methods for customizing all of these features.
 * <p/>
 * Additionally, the client will authenticate with the Opower authorization-service to acquire the necessary
 * oauth2 tokens. All you have to do is provide the credentials.
 *
 * @param <T> the type of the client being built
 * @author chris.phillips
 */
public class OpowerClientBuilder<T> {
    
    static {
        HystrixPlugins.getInstance().registerEventNotifier(new OpowerEventNotifier());
    }

    public static final String AUTH_SERVICE_NAME = "authorization-v1";
    private static final String VALID_CLIENT_NAME_PATTERN = "[a-zA-Z0-9\\-\\.]+";

    private static final MetricsProviderFactory METRICS_PROVIDER_FACTORY = FactoryLoaders.METRIC_PROVIDER.load();
    private static final SensuPublisherFactory SENSU_PUBLISHER_FACTORY = FactoryLoaders.SENSU_PUBLISHER.load();
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new GuavaModule())
            .registerModule(new JodaModule())
            .setDateFormat(new ISO8601DateFormat());
    private static final Logger LOG = LoggerFactory.getLogger(OpowerClientBuilder.class);

    private final ObjectMapper objectMapper = DEFAULT_OBJECT_MAPPER.copy();
    private final JacksonJsonProvider jacksonJsonProvider = new JacksonJsonProvider(this.objectMapper)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final OpowerResourceInterface<T> resourceInterface;
    private final UriProvider uriProvider;
    private final String serviceName;
    private final String clientId;

    private List<ClientRequestFilter> clientRequestFilters;
    private List<ClientErrorInterceptor> clientErrorInterceptors = ImmutableList
            .<ClientErrorInterceptor>of(new ExceptionMapperInterceptor());
    private ExponentialRetryStrategy retryStrategy = new ExponentialRetryStrategy();
    private Map<Method, HystrixCommandProperties.Setter> commandPropertiesMap = ImmutableMap.of();
    private Map<Method, HystrixThreadPoolProperties.Setter> threadPoolProperties = ImmutableMap.of();
    private Map<Method, Callable<?>> fallbacks = ImmutableMap.of();
    private int tokenTtlRefresh;
    private Optional<ServiceDiscovery<Void>> serviceDiscovery = Optional.absent();
    private Optional<BasicAuthCredentials> authorizationCredentials = Optional.absent();
    private Optional<UriProvider> authUriProvider = Optional.absent();
    private Optional<ConfigurationCallback<HttpParams>> httpClientParams = Optional.absent();
    private Optional<HystrixCommandProperties.Setter> defaultCommandProperties = Optional.absent();
    private Optional<HystrixThreadPoolProperties.Setter> defaultThreadPoolProperties = Optional.absent();
    private Optional<MetricsProvider> metricsProvider = Optional.absent();
    private Optional<SensuConfiguration.Setter> sensuConfiguration = Optional.of(new SensuConfiguration.Setter());

    /**
     * Creates an OpowerClientBuilder instance that will use an alternate UriProvider (rather than the default
     * CuratorUriProvider you get when using the other constructor). You should have a really good reason to use this
     * constructor.
     *
     * @param resourceInterface the resource interface
     * @param uriProvider       the UriProvider to use
     * @param serviceName       the name of the service you are consuming
     * @param clientId          the oauth2 clientId. If you are not authorizing these service calls for some reason you must
     *                          still provide a clientId that can be used to identify the traffic this client will generate in
     *                          metrics and alerts.
     */
    public OpowerClientBuilder(Class<T> resourceInterface, UriProvider uriProvider, String serviceName, String clientId) {
        this.resourceInterface = new OpowerResourceInterface<>(resourceInterface);
        this.serviceName = checkNotNull(serviceName);
        this.uriProvider = checkNotNull(uriProvider);
        this.metricsProvider = Optional.of(METRICS_PROVIDER_FACTORY.instance(this.serviceName));
        this.sensuConfiguration = Optional.of(new SensuConfiguration.Setter()
                                                     .filterOnClasses(ImmutableSet.<Class<?>>of(
                                                             resourceInterface)));
        this.clientRequestFilters = ImmutableList.of(
                new RequestIdFilter(),
                new ServiceNameClientRequestFilter(serviceName));
        checkArgument(clientId != null && clientId.matches(VALID_CLIENT_NAME_PATTERN),
                      String.format("You must provide a name that matches the pattern /%s/", VALID_CLIENT_NAME_PATTERN));
        this.clientId = clientId;
    }

    /**
     * Creates an OpowerClientBuilder instance that will use Zookeeper to look up server urls. This is
     * the preferred method of creating client instances.
     *
     * @param resourceInterface the resource interface
     * @param serviceDiscovery  The curator ServiceDiscovery instance that will be used to look up server urls
     * @param serviceName       The serviceName. This should match the name of the service as registered in Zookeeper
     * @param clientId          the oauth2 clientId. If you are not authorizing these service calls for some reason you must
     *                          still provide a clientId that can be used to identify the traffic this client will generate in
     *                          metrics and alerts.
     */
    public OpowerClientBuilder(Class<T> resourceInterface, ServiceDiscovery<Void> serviceDiscovery,
                               String serviceName, String clientId) {
        this(resourceInterface, new CuratorUriProvider(checkNotNull(serviceDiscovery), checkNotNull(serviceName)), serviceName,
             clientId);
        this.serviceDiscovery = Optional.of(serviceDiscovery);
    }


    /**
     * Enables authentication with the authorization-service. Will use the supplied credentials to obtain
     * an oauth2 access token.
     *
     * @param clientSecret the oauth2 client secret to use
     * @return the builder
     */
    public OpowerClientBuilder<T> clientSecret(String clientSecret) {
        checkArgument(clientSecret != null && clientSecret.trim().length() > 0, "You must provide a non-blank clientSecret");
        this.authorizationCredentials = Optional.of(new BasicAuthCredentials(this.clientId, clientSecret));
        return this;
    }

    /**
     * This overload is for scenarios where you aren't using the default UriProvider (curator based). In this
     * case you also need to specify how to obtain a UriProvider so that the client can obtain the access token.
     * 
     * @param clientSecret the oauth2 client secret to use
     * @param uriProvider  the UriProvider to use for accessing the authorization-service
     * @return the builder
     */
    public OpowerClientBuilder<T> clientSecret(String clientSecret, UriProvider uriProvider) {
        this.clientSecret(clientSecret);
        this.authUriProvider = Optional.of(uriProvider);
        return this;
    }

    /**
     * Adds a ClientRequestFilter to the list of filters to be processed before every request.
     *
     * @param clientRequestFilter the filter to add
     * @return the builder
     */
    public OpowerClientBuilder<T> addClientRequestFilter(ClientRequestFilter clientRequestFilter) {
        this.clientRequestFilters = ImmutableList.<ClientRequestFilter>builder()
                                                 .addAll(this.clientRequestFilters).add(clientRequestFilter).build();
        return this;
    }

    /**
     * Specify a new List of ClientErrorInterceptors to use instead of the default.
     *
     * @param clientErrorInterceptors the ClientErrorInterceptors to use.
     * @return the builder
     */
    public OpowerClientBuilder<T> setErrorInterceptors(List<ClientErrorInterceptor> clientErrorInterceptors) {
        this.clientErrorInterceptors = ImmutableList.copyOf(clientErrorInterceptors);
        return this;
    }

    /**
     * Provide a configuration callback to customize the ObjectMapper used for serialization.
     *
     * @param configurationCallback the callback to use
     * @return the builder
     */
    public OpowerClientBuilder<T> configureObjectMapper(ConfigurationCallback<ObjectMapper> configurationCallback) {
        configurationCallback.configure(this.objectMapper);
        return this;
    }

    /**
     * Provide a configuration callback to customize the JacksonJsonProvider used during serialization.
     *
     * @param configurationCallback the callback to use
     * @return the builder
     */
    public OpowerClientBuilder<T> configureJacksonJsonProvider(ConfigurationCallback<JacksonJsonProvider> 
                                                                       configurationCallback) {
        configurationCallback.configure(this.jacksonJsonProvider);
        return this;
    }

    /**
     * When the underlying http request receives a 503 response, the request can be automatically retried. Use this method
     * to configure how many retries and the delays between such retries.
     *
     * @param maxAttempts   the maximum number of times to attempt the requests (including retries). The default is 2 attempts
     *                      (1 retry).
     * @param sleepInterval the base interval to wait in between retries in milliseconds. This interval will increase 
     *                      exponentially
     *                      based on the number of times the request has been attempted. The default value is 1000 ms.
     * @return the builder
     */
    public OpowerClientBuilder configureRetries(final int maxAttempts, final long sleepInterval) {
        this.retryStrategy = new ExponentialRetryStrategy(maxAttempts, sleepInterval);
        return this;
    }

    /**
     * Set any parameters needed to configure the underlying httpclient instance.
     *
     * @param httpParamsConfigurationCallback callback that alters the HttpParams for the underlying http client
     * @return the builder
     */
    public OpowerClientBuilder<T> httpClientParams(ConfigurationCallback<HttpParams> httpParamsConfigurationCallback) {
        this.httpClientParams = Optional.of(httpParamsConfigurationCallback);
        return this;
    }

    /**
     * Disables automatic metric collection. You should have a good reason for disabling this.
     *
     * @return the builder
     */
    public OpowerClientBuilder<T> disableMetrics() {
        this.metricsProvider = Optional.absent();
        this.sensuConfiguration = Optional.absent();
        return this;
    }

    /**
     * Disables the publishing of metrics to sensu for ultimate collection in OpenTSDB.
     *
     * @return the builder
     */
    public OpowerClientBuilder<T> disableSensuPublishing() {
        this.sensuConfiguration = Optional.absent();
        return this;
    }

    /**
     * Provide custom settings for the metrics publisher.
     *
     * @param sensuConfiguration the SensuConfiguration to use
     * @return the builder
     */
    public OpowerClientBuilder<T> configureSensuPublisher(SensuConfiguration.Setter sensuConfiguration) {
        checkNotNull(sensuConfiguration);
        this.sensuConfiguration = Optional.of(sensuConfiguration
                                                      .filterOnClasses(ImmutableSet
                                                          .<Class<?>>of(this.resourceInterface.getInterface())));
        return this;
    }

    /**
     * Provide defaultCommandProperties that should be used for all methods on the ResourceInterface.
     *
     * @param defaultCommandProperties the HystrixCommandProperties.Setter to use
     * @return the builder
     */
    public OpowerClientBuilder<T> defaultCommandProperties(HystrixCommandProperties.Setter defaultCommandProperties) {
        this.defaultCommandProperties = Optional.of(defaultCommandProperties);
        return this;
    }

    /**
     * Provide HystrixCommandProperties defaults that should be used for the sepcified method.
     *
     * @param method     the method to configure
     * @param properties the HystrixCommandProperties.Setter to use as defaults
     * @return the builder
     */
    public OpowerClientBuilder<T> customCommandProperties(Method method, HystrixCommandProperties.Setter properties) {
        this.commandPropertiesMap = ImmutableMap.<Method, HystrixCommandProperties.Setter>builder()
                                                .putAll(this.commandPropertiesMap)
                                                .put(method, properties).build();
        return this;
    }

    /**
     * Provide HystrixThreadPoolProperties that should be used for all the methods on the ResourceInterface.
     *
     * @param threadPoolProperties the HystrixThreadPoolProperties.Setter to use
     * @return the builder
     */
    public OpowerClientBuilder<T> defaultThreadPoolProperties(HystrixThreadPoolProperties.Setter threadPoolProperties) {
        this.defaultThreadPoolProperties = Optional.of(threadPoolProperties);
        return this;
    }

    /**
     * Specify custom HystrixThreadPoolProperties for a specific method on the ResourceInterface.
     *
     * @param method                      the method to apply the HystrixThreadPoolProperties to
     * @param hystrixThreadPoolProperties the HystrixThreadPoolProperties to use
     * @return the HystrixClientBuilder
     */
    public OpowerClientBuilder<T> customThreadPoolProperties(Method method,
                                                             HystrixThreadPoolProperties.Setter hystrixThreadPoolProperties) {
        this.threadPoolProperties = ImmutableMap.<Method, HystrixThreadPoolProperties.Setter>builder()
                                                .putAll(this.threadPoolProperties)
                                                .put(method, hystrixThreadPoolProperties).build();
        return this;
    }

    /**
     * Provide a custom fallback to be used when the service request cannot be completed for any reason. By
     * default, the method will throw an exception if the request cannot be completed.
     *
     * @param method   the method to configure this fallback to
     * @param fallback the fallback to use
     * @return the builder
     */
    public OpowerClientBuilder<T> customFallback(Method method, Callable<?> fallback) {
        this.fallbacks = ImmutableMap.<Method, Callable<?>>builder().putAll(this.fallbacks).put(method, fallback).build();
        return this;
    }

    /**
     * The remaining duration of an Oauth2 token's lifetime, in seconds, for which to attempt to refresh the token.
     *
     * @param tokenTtlRefresh the remaining duration of an Oauth2 token's lifetime, in seconds, for which to attempt
     *                        to refresh the token.
     * @return the builder
     */
    public OpowerClientBuilder tokenTtlRefresh(int tokenTtlRefresh) {
        Preconditions.checkArgument(tokenTtlRefresh >= 0,
                                    "tokenTtlRefresh [%d] must be greater than or equal to 0",
                                    tokenTtlRefresh);
        this.tokenTtlRefresh = tokenTtlRefresh;
        return this;
    }

    private HttpClient prepareHttpClient() {
        AutoRetryHttpClient client =
                new org.apache.http.impl.client.AutoRetryHttpClient(
                        new DefaultHttpClient(new PoolingClientConnectionManager()),
                        this.retryStrategy);
        if (this.httpClientParams.isPresent()) {
            this.httpClientParams.get().configure(client.getParams());
        }
        return client;
    }

    private void setUpAuthorization() throws Exception {
        if (this.authorizationCredentials.isPresent()) {

            UriProvider uriProviderToUse = null;

            if (this.serviceDiscovery.isPresent()) {
                uriProviderToUse = new CuratorUriProvider(this.serviceDiscovery.get(), AUTH_SERVICE_NAME);
            } else {
                if (!this.authUriProvider.isPresent()) {
                    throw new IllegalStateException(
                            "You must specify an authUriProvider instance since you're not using curator");
                }
                uriProviderToUse = this.authUriProvider.get();
            }
            AccessTokenResource accessTokenResource = ClientBuilder
                    .instance(new OpowerResourceInterface<>(AccessTokenResource.class),
                              uriProviderToUse)
                    .clientErrorInterceptors(this.clientErrorInterceptors)
                    .executor(new ApacheHttpClient4Executor(prepareHttpClient(),
                                                            ImmutableList.of(new RequestIdFilter(),
                                                                             new ServiceNameClientRequestFilter(
                                                                                     AUTH_SERVICE_NAME),
                                                                             new AuthorizationClientRequestFilter(
                                                                                     this.authorizationCredentials.get()))))
                    .messageBodyProviders(this.jacksonJsonProvider, this.jacksonJsonProvider)
                    .build();

            addClientRequestFilter(
                    new Oauth2CredentialRequestingClientRequestFilter(
                            new Oauth2AccessTokenRequester(accessTokenResource,
                                                           this.tokenTtlRefresh)));
        }
    }
    
    private HystrixCommand.Setter buildDefaultHystrixSetter() {
        HystrixCommandGroupKey groupKey = HystrixCommandGroupKey.Factory.asKey(this.clientId);

        HystrixCommand.Setter defaultSetter = HystrixCommand.Setter.withGroupKey(groupKey);
        if (this.defaultCommandProperties.isPresent()) {
            defaultSetter.andCommandPropertiesDefaults(this.defaultCommandProperties.get());
        }
        if (this.defaultThreadPoolProperties.isPresent()) {
            defaultSetter.andThreadPoolPropertiesDefaults(this.defaultThreadPoolProperties.get());
        }   
        
        return defaultSetter;
    }
    
    private void configureCustomHystrixProperties(HystrixClientBuilder<T> clientBuilder) {
        for (Map.Entry<Method, HystrixCommandProperties.Setter> entry : this.commandPropertiesMap.entrySet()) {
            clientBuilder.customProperties(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Method, HystrixThreadPoolProperties.Setter> entry : this.threadPoolProperties.entrySet()) {
            clientBuilder.customThreadPoolProperties(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Method, Callable<?>> entry : this.fallbacks.entrySet()) {
            clientBuilder.customFallback(entry.getKey(), entry.getValue());
        }   
    }
    
    private T configureMetrics(T client) {
        T result = client;
        if (this.metricsProvider.isPresent()) {
            result = InstrumentedClientInvocationHandler.instrument(this.resourceInterface.getInterface(),
                                                                    client, this.metricsProvider.get());
            registerForShortCircuitSensuChecks();

            configureSensuPublishing();
        }
        return result;
    }
    
    private void configureSensuPublishing() {
        if (this.sensuConfiguration.isPresent()) {
            SensuConfiguration configToUse = new SensuConfiguration(this.sensuConfiguration.get()
                                                                                           .setClientId(this.clientId));
            SENSU_PUBLISHER_FACTORY.instance(this.serviceName, configToUse).start();
        }    
    }
    
    private void registerForShortCircuitSensuChecks() {
        for (Method m : this.resourceInterface.getInterface().getMethods()) {
            HystrixCommandKey commandKey = HystrixClientBuilder.keyForMethod(m);
            OpowerEventNotifier.registerClient(commandKey, this.clientId, this.metricsProvider.get());
        }   
    }

    /**
     * Build the client proxy.
     *
     * @return the client proxy ready to use
     * @throws Exception in case of any failure
     */
    public T build() throws Exception {
        setUpAuthorization();

        HystrixClientBuilder<T> clientBuilder = new HystrixClientBuilder<>(this.resourceInterface,
                                                                           this.uriProvider,
                                                                           buildDefaultHystrixSetter());
        configureCustomHystrixProperties(clientBuilder);

        clientBuilder.executor(new ApacheHttpClient4Executor(prepareHttpClient(), this.clientRequestFilters))
                     .clientErrorInterceptors(this.clientErrorInterceptors)
                     .messageBodyProviders(this.jacksonJsonProvider, this.jacksonJsonProvider);
        
        return configureMetrics(clientBuilder.build());
        
    }


}
