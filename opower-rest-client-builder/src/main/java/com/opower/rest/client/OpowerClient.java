package com.opower.rest.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.opower.auth.resources.oauth2.AccessTokenResource;
import com.opower.metrics.FactoryLoaders;
import com.opower.metrics.MetricsProvider;
import com.opower.metrics.MetricsProviderFactory;
import com.opower.metrics.SensuConfiguration;
import com.opower.metrics.SensuPublisherFactory;
import com.opower.rest.client.curator.CuratorUriProvider;
import com.opower.rest.client.envelope.EnvelopeErrorInterceptor;
import com.opower.rest.client.envelope.EnvelopeJacksonProvider;
import com.opower.rest.client.filters.RequestIdFilter;
import com.opower.rest.client.filters.ServiceNameClientRequestFilter;
import com.opower.rest.client.filters.auth.AuthorizationClientRequestFilter;
import com.opower.rest.client.filters.auth.Oauth2AccessTokenRequester;
import com.opower.rest.client.filters.auth.Oauth2CredentialRequestingClientRequestFilter;
import com.opower.rest.client.generator.core.Client;
import com.opower.rest.client.generator.core.ClientErrorInterceptor;
import com.opower.rest.client.generator.core.ClientRequestFilter;
import com.opower.rest.client.generator.core.UriProvider;
import com.opower.rest.client.generator.executors.ApacheHttpClient4Executor;
import com.opower.rest.client.generator.hystrix.HystrixClient;
import com.opower.rest.client.http.AutoRetryAuthorizationRefreshHttpClient;
import com.opower.rest.client.http.ExponentialRetryStrategy;
import com.opower.rest.client.http.UnauthorizedRetryStrategy;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.AutoRetryHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpParams;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class to make the return types of the inherited builders work correctly.
 * @author chris.phillips
 * @param <T> the type of the client to be created
 * @param <B> the type of the concrete builder
 */

public abstract class OpowerClient<T, B extends OpowerClient<T, B>> extends HystrixClient<T, B> {
    public static final String AUTH_SERVICE_NAME = "authorization-v1";

    private static final int DEFAULT_TOKEN_TTL_REFRESH = 2;
    private static final int THREAD_POOL_QUEUEING_FACTOR = 10;
    private static final int DEFAULT_HYSTRIX_THREAD_POOL_SIZE = 10;
    private static final String VALID_CLIENT_NAME_PATTERN = "[a-zA-Z0-9\\-\\.]+";
    private static final MetricsProviderFactory METRICS_PROVIDER_FACTORY = FactoryLoaders.METRIC_PROVIDER.load();
    private static final SensuPublisherFactory SENSU_PUBLISHER_FACTORY = FactoryLoaders.SENSU_PUBLISHER.load();
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new GuavaModule())
            .registerModule(new JodaModule())
            .setDateFormat(new ISO8601DateFormat());
    private static final int DEFAULT_TOKEN_REFRESH_ATTEMPTS = 1;
    private static final int DEFAULT_TOKEN_REFRESH_ATTEMPT_INTERVAL = 100;

    private final ObjectMapper objectMapper = DEFAULT_OBJECT_MAPPER.copy();
    private JacksonJsonProvider jacksonJsonProvider = new JacksonJsonProvider(this.objectMapper)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final String serviceName;
    private final String clientId;

    private List<ClientRequestFilter> clientRequestFilters;
    private ExponentialRetryStrategy retryStrategy = new ExponentialRetryStrategy();
    private int tokenTtlRefresh = DEFAULT_TOKEN_TTL_REFRESH;
    private Optional<ServiceDiscovery<Void>> serviceDiscovery = Optional.absent();
    private Optional<String> clientSecret = Optional.absent();
    private Optional<UriProvider> authUriProvider = Optional.absent();
    private Optional<ConfigurationCallback<HttpParams>> httpClientParams = Optional.absent();
    private Optional<MetricsProvider> metricsProvider = Optional.absent();
    private Optional<SensuConfiguration.Setter> sensuConfiguration = Optional.of(new SensuConfiguration.Setter());
    private Predicate<String> metricPublishingFilter = Predicates.<String>alwaysTrue();
    private int tokenRefreshAttempts = DEFAULT_TOKEN_REFRESH_ATTEMPTS;
    private int tokenRefreshAttemptInterval = DEFAULT_TOKEN_REFRESH_ATTEMPT_INTERVAL;

    /**
     * Creates an OpowerClient instance that will use an alternate UriProvider (rather than the default
     * CuratorUriProvider you get when using the other constructor). You should have a really good reason to use this
     * constructor.
     *
     * @param resourceInterface the resource interface
     * @param uriProvider       the UriProvider to use
     * @param serviceName       the name of the service you are consuming
     * @param clientId          the oauth2 clientId. If you are not authorizing these service calls for some reason you must
     *                          still provide a clientId that can be used to identify the traffic this client will generate in
     *                          metrics and alerts.
     * @deprecated serviceName is no longer required if the ResourceInterface you want to use has been annotated
     *             with @ResourceMetadata. Encourage the service writer to update to the latest version of
     *             rest-client tools!
     */
    @Deprecated
    protected OpowerClient(Class<T> resourceInterface, UriProvider uriProvider, String serviceName, String clientId) {
        this(new OpowerResourceInterface<>(resourceInterface), uriProvider, serviceName, clientId);
    }

    /**
     * Creates an OpowerClient instance that will use Zookeeper to look up server urls. This is now deprecated. We have
     * released synapse-lite and you should instead use the other non deprecated constructor with a SimpleUriProvider
     * pointed at synapse-lite instead. For example:
     *
     *      new OpowerClient.Builder(someClass, new SimpleUriProvider("http://dev-synapse.va.opower.it"), "clientId);
     *
     * @param resourceInterface the resource interface
     * @param serviceDiscovery  The curator ServiceDiscovery instance that will be used to look up server urls
     * @param serviceName       The serviceName. This should match the name of the service as registered in Zookeeper
     * @param clientId          the oauth2 clientId. If you are not authorizing these service calls for some reason you must
     *                          still provide a clientId that can be used to identify the traffic this client will generate in
     *                          metrics and alerts.
     * @deprecated serviceName is no longer required if the ResourceInterface you want to use has been annotated
     *             with @ResourceMetadata. Encourage the service writer to update to the latest version of
     *             rest-client tools!
     */
    @Deprecated
    protected OpowerClient(Class<T> resourceInterface, ServiceDiscovery<Void> serviceDiscovery,
                           String serviceName, String clientId) {
        this(new OpowerResourceInterface<>(resourceInterface), serviceDiscovery, serviceName, clientId);
    }

    /**
     * Creates an OpowerClient instance that will use Zookeeper to look up server urls. This is
     * the preferred method of creating client instances.
     *
     * @param resourceInterface the OpowerResourceInterface to use
     * @param serviceDiscovery  The curator ServiceDiscovery instance that will be used to look up server urls
     * @param serviceName       The serviceName. This should match the name of the service as registered in Zookeeper
     * @param clientId          the oauth2 clientId. If you are not authorizing these service calls for some reason you must
     *                          still provide a clientId that can be used to identify the traffic this client will generate in
     *                          metrics and alerts.
     * @deprecated serviceName is no longer required if the ResourceInterface you want to use has been annotated
     *             with @ResourceMetadata. Encourage the service writer to update to the latest version of
     *             rest-client tools!
     */
    @Deprecated
    protected OpowerClient(OpowerResourceInterface<T> resourceInterface, ServiceDiscovery<Void> serviceDiscovery,
                           String serviceName, String clientId) {
        this(resourceInterface, new CuratorUriProvider(checkNotNull(serviceDiscovery), checkNotNull(serviceName)),
             serviceName, clientId);
        this.serviceDiscovery = Optional.of(serviceDiscovery);
    }
    /**
     * Creates an OpowerClient instance that will use an alternate UriProvider (rather than the default
     * CuratorUriProvider you get when using the other constructor). You should have a really good reason to use this
     * constructor.
     *
     * @param resourceInterface the OpowerResourceInterface to use
     * @param uriProvider       the UriProvider to use
     * @param serviceName       the name of the service you are consuming
     * @param clientId          the oauth2 clientId. If you are not authorizing these service calls for some reason you must
     *                          still provide a clientId that can be used to identify the traffic this client will generate in
     *                          metrics and alerts.
     */
    protected OpowerClient(OpowerResourceInterface<T> resourceInterface, UriProvider uriProvider,
                           String serviceName, String clientId) {
        super(resourceInterface, uriProvider, groupKey(clientId));
        this.serviceName = checkNotNull(serviceName, "The ResourceInterface must be annotated with @ResourceMetadata or you must" +
                                                     " use another constructor to supply the serviceName");
        this.metricsProvider = Optional.of(METRICS_PROVIDER_FACTORY.getInstance(String.format("%s.client", this.serviceName)));
        this.sensuConfiguration = Optional.of(new SensuConfiguration.Setter());
        this.clientRequestFilters = ImmutableList.of(
                new RequestIdFilter(),
                new ServiceNameClientRequestFilter(serviceName));
        this.clientErrorInterceptors = ImmutableList
                .<ClientErrorInterceptor>of(new ExceptionMapperInterceptor());
        this.clientId = clientId;
    }


    private static HystrixCommandGroupKey groupKey(String clientId) {
        checkArgument(clientId != null && clientId.matches(VALID_CLIENT_NAME_PATTERN),
                      String.format("You must provide a name that matches the pattern /%s/", VALID_CLIENT_NAME_PATTERN));
        return HystrixCommandGroupKey.Factory.asKey(clientId);
    }

    /**
     * Enables authentication with the authorization-service. Will use the supplied credentials to obtain
     * an oauth2 access token.
     *
     * @param clientSecret the oauth2 client secret to use
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B clientSecret(String clientSecret) {
        checkArgument(clientSecret == null || clientSecret.trim().length() > 0, "You must provide a non-blank clientSecret");
        this.clientSecret = Optional.fromNullable(clientSecret);
        return (B) this;
    }

    /**
     * This overload is for scenarios where you aren't using the default UriProvider (curator based). In this
     * case you also need to specify how to obtain a UriProvider so that the client can obtain the access token.
     *
     * @param clientSecret the oauth2 client secret to use
     * @param uriProvider  the UriProvider to use for accessing the authorization-service
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B clientSecret(String clientSecret, UriProvider uriProvider) {
        this.clientSecret(clientSecret);
        this.authUriProvider = Optional.of(uriProvider);
        return (B) this;
    }

    /**
     * Adds a ClientRequestFilter to the list of filters to be processed before every request.
     *
     * @param clientRequestFilter the filter to add
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B addClientRequestFilter(ClientRequestFilter clientRequestFilter) {
        this.clientRequestFilters = ImmutableList.<ClientRequestFilter>builder()
                                                 .addAll(this.clientRequestFilters).add(clientRequestFilter).build();
        return (B) this;
    }

    /**
     * Provide a configuration callback to customize the ObjectMapper used for serialization.
     *
     * @param configurationCallback the callback to use
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B configureObjectMapper(ConfigurationCallback<ObjectMapper> configurationCallback) {
        configurationCallback.configure(this.objectMapper);
        return (B) this;
    }

    /**
     * Provide a configuration callback to customize the JacksonJsonProvider used during serialization.
     *
     * @param configurationCallback the callback to use
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B configureJacksonJsonProvider(ConfigurationCallback<JacksonJsonProvider>
                                                                       configurationCallback) {
        configurationCallback.configure(this.jacksonJsonProvider);
        return (B) this;
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
    @SuppressWarnings("unchecked")
    public B configureRetries(final int maxAttempts, final long sleepInterval) {
        this.retryStrategy = new ExponentialRetryStrategy(maxAttempts, sleepInterval);
        return (B) this;
    }

    /**
     * Set any parameters needed to configure the underlying httpclient instance.
     *
     * @param httpParamsConfigurationCallback callback that alters the HttpParams for the underlying http client
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B httpClientParams(ConfigurationCallback<HttpParams> httpParamsConfigurationCallback) {
        this.httpClientParams = Optional.of(httpParamsConfigurationCallback);
        return (B) this;
    }

    /**
     * Disables automatic metric collection. You should have a good reason for disabling this.
     *
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B disableMetrics() {
        this.metricsProvider = Optional.absent();
        this.sensuConfiguration = Optional.absent();
        return (B) this;
    }

    /**
     * Disables the publishing of metrics to sensu for ultimate collection in OpenTSDB.
     *
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B disableSensuPublishing() {
        this.sensuConfiguration = Optional.absent();
        return (B) this;
    }

    /**
     * The default is to send all metrics associated with this client. If you wish to filter out some of the metrics you can do
     * so by providing a filter predicate here. The filter operates on metrics by name.
     * @param filter the filter to use
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B sensuPublishingFilter(Predicate<String> filter) {
        this.metricPublishingFilter = checkNotNull(filter);
        return (B) this;
    }

    /**
     * Provide custom settings for the Sensu metrics publisher. Note, there is only one SensuPublisher instance per JVM.
     * If you have already configured the publishing elsewhere then calling this method will have no effect. In archmage services
     * particularly calling this method is unnecessary and will have no effect since archmage manages the SensuPublishing and you
     * would apply any special configuration settings there.
     *
     * This method is mainly a convenience for those who are creating clients outside of archmage and don't want to set up all
     * the publishing manually. In such a case, if something about the publishing needs tweaked (publish interval etc)
     * it can be accomplished here.
     *
     * @param callback the ConfigurationCallback to use
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B configureSensuPublisher(ConfigurationCallback<SensuConfiguration.Setter> callback) {
        checkNotNull(callback);
        if (!this.sensuConfiguration.isPresent()) {
            throw new IllegalStateException("Cannot configure Sensu publishing if you have disabled it previously.");
        }
        callback.configure(this.sensuConfiguration.get());
        return (B) this;
    }

    /**
     * The remaining duration of an Oauth2 token's lifetime, in seconds, for which to attempt to refresh the token.
     *
     * @param tokenTtlRefresh the remaining duration of an Oauth2 token's lifetime, in seconds, for which to attempt
     *                        to refresh the token.
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B tokenTtlRefreshSeconds(int tokenTtlRefresh) {
        checkArgument(tokenTtlRefresh >= 0, "tokenTtlRefresh [%d] must be greater than or equal to 0", tokenTtlRefresh);
        this.tokenTtlRefresh = tokenTtlRefresh;
        return (B) this;
    }

    /**

     * This allows the client to unwrap response and error objects wrapped according to the X-OPOWER-JsonEnvelope
     * specification.
     *
     * Invoking this method changes your jackson json provider to:
     * {@link com.opower.rest.client.envelope.EnvelopeJacksonProvider} and sets the client error interceptor to:
     * {@link com.opower.rest.client.envelope.EnvelopeErrorInterceptor}.
     *
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B useJsonEnvelopes() {
        this.jacksonJsonProvider = new EnvelopeJacksonProvider();
        this.clientErrorInterceptors = ImmutableList.<ClientErrorInterceptor>of(new EnvelopeErrorInterceptor());
        return (B) this;
    }
    
    /**
     * The number of times to attempt to refresh the access token when a request results in a 401 Unauthorized status.
     *
     * @param tokenRefreshAttempts the number of times to attempt to refresh the access token when a request results in a 
     *                             401 Unauthorized status
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B tokenRefreshAttempts(int tokenRefreshAttempts) {
        this.tokenRefreshAttempts = tokenRefreshAttempts;
        return (B) this;
    }

    /**
     * The interval, in milliseconds, until a failed request should have it's access token refreshed.
     *
     * @param tokenRefreshAttemptInterval the interval, in milliseconds, until a failed request should have it's access token 
     *                                    refreshed.
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public B tokenRefreshAttemptInterval(int tokenRefreshAttemptInterval) {
        this.tokenRefreshAttemptInterval = tokenRefreshAttemptInterval;
        return (B) this;
    }

    /**
     * Visible for testing.
     * @return the HttpClientInstance to use.
     */
    HttpClient prepareHttpClient() {
        int totalSize = 0;
        for (HystrixThreadPoolProperties.Setter setter : this.threadPoolPropertiesMap.values()) {
            totalSize += setter.getCoreSize() == null ? DEFAULT_HYSTRIX_THREAD_POOL_SIZE : setter.getCoreSize();
        }
        PoolingClientConnectionManager poolingClientConnectionManager = new PoolingClientConnectionManager();
        poolingClientConnectionManager.setMaxTotal(totalSize);
        poolingClientConnectionManager.setDefaultMaxPerRoute(totalSize);

        AutoRetryHttpClient client = new AutoRetryHttpClient(new DefaultHttpClient(poolingClientConnectionManager),
                                                             this.retryStrategy);
        if (this.httpClientParams.isPresent()) {
            this.httpClientParams.get().configure(client.getParams());
        }
        return client;
    }

    /**
     * Visible for testing.
     * @return the Map of methods to HystrixCommandProperties.Setter
     */
    Map<Method, HystrixCommandProperties.Setter> getCommandPropertiesMap() {
        return this.commandPropertiesMap;
    }

    /**
     * Visible for testing.
     * @return the Map of methods to HystrixThreadPoolProperties.Setter
     */
    Map<Method, HystrixThreadPoolProperties.Setter> getThreadPoolPropertiesMap() {
        return this.threadPoolPropertiesMap;
    }

    private Oauth2AccessTokenRequester createOauth2AccessTokenRequester() {
        UriProvider uriProviderToUse = this.uriProvider;

        if (this.serviceDiscovery.isPresent()) {
            uriProviderToUse = new CuratorUriProvider(this.serviceDiscovery.get(), AUTH_SERVICE_NAME);
        } else if (this.authUriProvider.isPresent()) {
            uriProviderToUse = this.authUriProvider.get();
        }

        BasicAuthCredentials credentials = new BasicAuthCredentials(this.clientId, this.clientSecret.get());
        AccessTokenResource accessTokenResource = new Client.Builder<>(new OpowerResourceInterface<>(AccessTokenResource.class),
                                                                       uriProviderToUse)
                .clientErrorInterceptors(this.clientErrorInterceptors)
                .executor(new ApacheHttpClient4Executor(prepareHttpClient(),
                                                        ImmutableList.of(new RequestIdFilter(),
                                                                         new ServiceNameClientRequestFilter(
                                                                                 AUTH_SERVICE_NAME),
                                                                         new AuthorizationClientRequestFilter(credentials))))
                .registerProviderInstance(this.jacksonJsonProvider)
                .build();

        return new Oauth2AccessTokenRequester(accessTokenResource, this.tokenTtlRefresh);
    }

    private T configureMetrics(T client) {
        T result = client;
        if (this.metricsProvider.isPresent()) {
            result = InstrumentedClientInvocationHandler.instrument(this.resourceInterface.getInterface(),
                                                                    client, this.metricsProvider.get());
            registerForShortCircuitGauges();

            configureSensuPublishing();
        }
        return result;
    }

    private void configureSensuPublishing() {
        if (this.sensuConfiguration.isPresent()) {
            SensuConfiguration configToUse = new SensuConfiguration(this.sensuConfiguration.get());
            SENSU_PUBLISHER_FACTORY.getInstance(configToUse).startPublishingFor(this.metricsProvider.get(),
                String.format("service.client.%s.%s", this.serviceName, this.clientId),
                this.metricPublishingFilter);
        }
    }

    private void registerForShortCircuitGauges() {
        for (HystrixCommandKey commandKey : super.commandKeyMap.values()) {
            registerClient(commandKey, this.clientId, this.metricsProvider.get());
        }
    }

    private void registerClient(final HystrixCommandKey key, String clientName, MetricsProvider metricsProvider) {
        checkArgument(clientName != null && clientName.trim().length() > 0);
        metricsProvider.gauge(String.format("%s.circuitbreaker.open", key.name()), new MetricsProvider.Gauge<Integer>() {
            @Override
            public Integer getValue() {
                HystrixCircuitBreaker hcb = HystrixCircuitBreaker.Factory.getInstance(key);
                if (hcb == null) {
                    return 0;
                } else {
                    // if we don't allow requests, it's because the circuit breaker is effectively open.
                    // That could be because the breaker was forced open, or because it was tripped.
                    return hcb.allowRequest() ? 0 : 1;
                }
            }
        });
    }

    /**
     *
     * Thread Pool queueing:
     * Hystrix defaults: http://goo.gl/yNI5KU
     * Spring defaults: http://goo.gl/BsTL1A
     * It seems to have surprised people that we use the SynchronousQueue by default.
     * To allow for dynamic sizing, Hystrix provides the queueSizeRejectionThreshold setting.
     *
     * Visible for testing.
     */
    void hystrixCommandDefaults() {

        threadPoolProperties(new ConfigurationCallback<HystrixThreadPoolProperties.Setter>() {
            @Override
            public void configure(HystrixThreadPoolProperties.Setter setter) {

                if (setter.getMaxQueueSize() == null) {
                    setter.withMaxQueueSize(Integer.MAX_VALUE);
                }

                if (setter.getQueueSizeRejectionThreshold() == null) {
                    int corePoolSize = setter.getCoreSize() == null ? DEFAULT_HYSTRIX_THREAD_POOL_SIZE : setter.getCoreSize();
                    // This is the effective queue max size and can be dynamically configured.
                    setter.withQueueSizeRejectionThreshold(corePoolSize * THREAD_POOL_QUEUEING_FACTOR);
                }
            }
        });

        commandProperties(new ConfigurationCallback<HystrixCommandProperties.Setter>() {
            @Override
            public void configure(HystrixCommandProperties.Setter setter) {
                // this minimum timeout allows the retry strategy to fully execute before the
                // hystrix command times out. The extra second is for the last request
                long minTimeout = OpowerClient.this.retryStrategy.getMaxCumulativeInterval()
                                  + TimeUnit.SECONDS.toMillis(1);
                Integer currentSetting = setter.getExecutionIsolationThreadTimeoutInMilliseconds();
                if (currentSetting == null || currentSetting < minTimeout) {
                    setter.withExecutionIsolationThreadTimeoutInMilliseconds((int) minTimeout);
                }
            }
        });
    }

    /**
     * Build the client proxy.
     *
     * @return the client proxy ready to use
     */
    @Override
    public T build()  {
        HttpClient client = prepareHttpClient();
        if (this.clientSecret.isPresent()) {
            Oauth2AccessTokenRequester oauth2AccessTokenRequester = createOauth2AccessTokenRequester();
            addClientRequestFilter(new Oauth2CredentialRequestingClientRequestFilter(oauth2AccessTokenRequester));
            ServiceUnavailableRetryStrategy unauthorizedRetryStrategy 
                = new UnauthorizedRetryStrategy(this.tokenRefreshAttempts, this.tokenRefreshAttemptInterval);
            client = new AutoRetryAuthorizationRefreshHttpClient(client,
                                                                 oauth2AccessTokenRequester,
                                                                 unauthorizedRetryStrategy,
                                                                 this.tokenRefreshAttempts);
        }

        hystrixCommandDefaults();

        super.executor(new ApacheHttpClient4Executor(client, this.clientRequestFilters))
             .clientErrorInterceptors(this.clientErrorInterceptors)
             .registerProviderInstance(this.jacksonJsonProvider);
        return configureMetrics(super.build());
    }

    /**
     * Builder class used to create rest client proxies with special care take to provide sensible
     * defaults for the Opower landscape. By default, the client will
     * - use curator to resolve server urls,
     * - collect timing metrics
     * - publish metrics to Sensu for reporting in OpenTSDB
     * - execute each method as a customizable HystrixCommand
     * - use Jackson for serialization
     * <p/>
     * There are methods for customizing all of these features.
     * <p/>
     * Additionally, the client will authenticate with the Opower authorization-service to acquire the necessary
     * oauth2 tokens. All you have to do is provide the client id and client secret via the appropriate constructor
     * and method.
     *
     * @param <T> the type of the client being built
     * @author chris.phillips
     */
    public static final class Builder<T> extends OpowerClient<T, Builder<T>> {

        /**
         * Creates an OpowerClient.Builder instance that will use an alternate UriProvider (rather than the default
         * CuratorUriProvider you get when using the other constructor). You should have a really good reason to use this
         * constructor.
         *
         * @param resourceInterface The resource interface
         * @param uriProvider       The UriProvider to use
         * @param serviceName       The name of the service you are consuming
         * @param clientId          The oauth2 clientId. If you are not authorizing these service calls for some reason you must
         *                          still provide a clientId that can be used to identify the traffic this client will generate in
         *                          metrics and alerts.
         * @deprecated serviceName is no longer required if the ResourceInterface you want to use has been annotated
         *             with @ResourceMetadata. Encourage the service writer to update to the latest version of
         *             rest-client tools!
         */
        @Deprecated
        public Builder(Class<T> resourceInterface, UriProvider uriProvider, String serviceName, String clientId) {
            super(resourceInterface, uriProvider, serviceName, clientId);
        }

        /**
         * Creates an OpowerClient.Builder instance that will use an alternate UriProvider (rather than the default
         * CuratorUriProvider you get when using the other constructor). You should have a really good reason to use this
         * constructor. This constructor allows you to use a customized OpowerResourceInterface
         *
         * @param resourceInterface The OpowerResourceInterface to use.
         * @param uriProvider       The UriProvider to use
         * @param serviceName       The name of the service you are consuming
         * @param clientId          The oauth2 clientId. If you are not authorizing these service calls for some reason you must
         *                          still provide a clientId that can be used to identify the traffic this client will generate in
         *                          metrics and alerts.
         * @deprecated serviceName is no longer required if the ResourceInterface you want to use has been annotated
         *             with @ResourceMetadata. Encourage the service writer to update to the latest version of
         *             rest-client tools!
         */
        @Deprecated
        public Builder(OpowerResourceInterface<T> resourceInterface, UriProvider uriProvider, String serviceName, String clientId) {
            super(resourceInterface, uriProvider, serviceName, clientId);
        }

        /**
         * This is now deprecated since we have released synapse-lite. You should instead use the other non deprecated
         * constructor with a SimpleUriProvider pointed at synapse-lite. For example:
         *
         *      new OpowerClient.Builder(someClass, new SimpleUriProvider("http://dev-synapse.va.opower.it"), "clientId);
         *
         * @param resourceInterface the resource interface
         * @param serviceDiscovery  The curator ServiceDiscovery instance that will be used to look up server urls
         * @param serviceName       The serviceName. This should match the name of the service as registered in Zookeeper
         * @param clientId          the oauth2 clientId. If you are not authorizing these service calls for some reason you must
         *                          still provide a clientId that can be used to identify the traffic this client will generate in
         *                          metrics and alerts.
         *
         * @deprecated serviceName is no longer required if the ResourceInterface you want to use has been annotated
         *             with @ResourceMetadata. Encourage the service writer to update to the latest version of
         *             rest-client tools!
         */
        @Deprecated
        public Builder(Class<T> resourceInterface,
                       ServiceDiscovery<Void> serviceDiscovery, String serviceName, String clientId) {
            super(resourceInterface, serviceDiscovery, serviceName, clientId);
        }

        /**
         * Creates an OpowerClient.Builder instance that will use Zookeeper to look up server urls. This is
         * the preferred method of creating client instances if you have to create your own OpowerResourceInterface instance
         * with custom package names allowed.
         *
         * @param resourceInterface the OpowerResourceInterface to use.
         * @param serviceDiscovery  The curator ServiceDiscovery instance that will be used to look up server urls
         * @param serviceName       The serviceName. This should match the name of the service as registered in Zookeeper
         * @param clientId          the oauth2 clientId. If you are not authorizing these service calls for some reason you must
         *                          still provide a clientId that can be used to identify the traffic this client will generate in
         *                          metrics and alerts.
         *
         * @deprecated serviceName is no longer required if the ResourceInterface you want to use has been annotated
         *             with @ResourceMetadata. Encourage the service writer to update to the latest version of
         *             rest-client tools!
         */
        @Deprecated
        public Builder(OpowerResourceInterface<T> resourceInterface, ServiceDiscovery<Void> serviceDiscovery,
                       String serviceName, String clientId) {
            super(resourceInterface, serviceDiscovery, serviceName, clientId);
        }

        /**
         * Create a client proxy Builder. The supplied class must be annotated with the @ResourceMetadata annotation.
         * @param resourceInterface the OpowerResourceInterface to use.
         * @param uriProvider       The UriProvider to use
         * @param clientId          the oauth2 clientId. If you are not authorizing these service calls for some reason you must
         *                          still provide a clientId that can be used to identify the traffic this client will generate in
         *                          metrics and alerts.
         */
        public Builder(Class<T> resourceInterface, UriProvider uriProvider, String clientId) {
            this(new OpowerResourceInterface<T>(resourceInterface), uriProvider, clientId);
        }

        /**
         * Create a client proxy Builder. The supplied class must be annotated with the @ResourceMetadata annotation.
         * @param resourceInterface the OpowerResourceInterface to use.
         * @param uriProvider       The UriProvider to use
         * @param clientId          the oauth2 clientId. If you are not authorizing these service calls for some reason you must
         *                          still provide a clientId that can be used to identify the traffic this client will generate in
         *                          metrics and alerts.
         */
        public Builder(OpowerResourceInterface<T> resourceInterface, UriProvider uriProvider, String clientId) {
            super(resourceInterface, uriProvider, resourceInterface.getServiceName().orNull(), clientId);
        }
    }
}
