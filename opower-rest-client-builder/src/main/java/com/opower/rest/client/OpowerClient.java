package com.opower.rest.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
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
import com.opower.rest.client.generator.core.Client;
import com.opower.rest.client.generator.core.ClientErrorInterceptor;
import com.opower.rest.client.generator.core.ClientRequestFilter;
import com.opower.rest.client.generator.core.UriProvider;
import com.opower.rest.client.generator.executors.ApacheHttpClient4Executor;
import com.opower.rest.client.generator.hystrix.HystrixClient;
import com.opower.rest.client.http.ExponentialRetryStrategy;
import com.opower.rest.client.hystrix.OpowerEventNotifier;
import java.util.List;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.http.client.HttpClient;
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
    private static final String VALID_CLIENT_NAME_PATTERN = "[a-zA-Z0-9\\-\\.]+";
    private static final MetricsProviderFactory METRICS_PROVIDER_FACTORY = FactoryLoaders.METRIC_PROVIDER.load();
    private static final SensuPublisherFactory SENSU_PUBLISHER_FACTORY = FactoryLoaders.SENSU_PUBLISHER.load();
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new GuavaModule())
            .registerModule(new JodaModule())
            .setDateFormat(new ISO8601DateFormat());

    private final ObjectMapper objectMapper = DEFAULT_OBJECT_MAPPER.copy();
    private final JacksonJsonProvider jacksonJsonProvider = new JacksonJsonProvider(this.objectMapper)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final String serviceName;
    private final String clientId;

    private List<ClientRequestFilter> clientRequestFilters;
    private ExponentialRetryStrategy retryStrategy = new ExponentialRetryStrategy();
    private int tokenTtlRefresh = DEFAULT_TOKEN_TTL_REFRESH;
    private Optional<ServiceDiscovery<Void>> serviceDiscovery = Optional.absent();
    private Optional<BasicAuthCredentials> authorizationCredentials = Optional.absent();
    private Optional<UriProvider> authUriProvider = Optional.absent();
    private Optional<ConfigurationCallback<HttpParams>> httpClientParams = Optional.absent();
    private Optional<MetricsProvider> metricsProvider = Optional.absent();
    private Optional<SensuConfiguration.Setter> sensuConfiguration = Optional.of(new SensuConfiguration.Setter());

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
     */
    protected OpowerClient(Class<T> resourceInterface, UriProvider uriProvider, String serviceName, String clientId) {
        this(new OpowerResourceInterface<>(resourceInterface), uriProvider, serviceName, clientId);
    }

    /**
     * Creates an OpowerClient instance that will use Zookeeper to look up server urls. This is
     * the preferred method of creating client instances.
     *
     * @param resourceInterface the resource interface
     * @param serviceDiscovery  The curator ServiceDiscovery instance that will be used to look up server urls
     * @param serviceName       The serviceName. This should match the name of the service as registered in Zookeeper
     * @param clientId          the oauth2 clientId. If you are not authorizing these service calls for some reason you must
     *                          still provide a clientId that can be used to identify the traffic this client will generate in
     *                          metrics and alerts.
     */
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
     */
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
        this.serviceName = checkNotNull(serviceName);
        this.metricsProvider = Optional.of(METRICS_PROVIDER_FACTORY.getInstance(String.format("%s.client", this.serviceName)));
        this.sensuConfiguration = Optional.of(new SensuConfiguration.Setter()
                                                      .filterOnClasses(ImmutableSet.<Class<?>>of(
                                                              resourceInterface.getInterface())));
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
        checkArgument(clientSecret != null && clientSecret.trim().length() > 0, "You must provide a non-blank clientSecret");
        this.authorizationCredentials = Optional.of(new BasicAuthCredentials(this.clientId, clientSecret));
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
     * Provide custom settings for the Sensu metrics publisher.
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
        Preconditions.checkArgument(tokenTtlRefresh >= 0,
                                    "tokenTtlRefresh [%d] must be greater than or equal to 0",
                                    tokenTtlRefresh);
        this.tokenTtlRefresh = tokenTtlRefresh;
        return (B) this;
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

    private void setUpAuthorization() {
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
            AccessTokenResource accessTokenResource = new Client.Builder<>(new OpowerResourceInterface<>(AccessTokenResource.class),
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
            SensuConfiguration configToUse = new SensuConfiguration(this.sensuConfiguration.get());
            SENSU_PUBLISHER_FACTORY.getInstance(configToUse).startPublishingFor(this.metricsProvider.get(),
                String.format("service.client.%s.%s", this.serviceName, this.clientId),
                Predicates.<String>alwaysTrue());
        }
    }

    private void registerForShortCircuitSensuChecks() {
        for (HystrixCommandKey commandKey : super.commandKeyMap.values()) {
            OpowerEventNotifier.registerClient(commandKey, this.clientId, this.metricsProvider.get());
        }
    }

    /**
     * Build the client proxy.
     *
     * @return the client proxy ready to use
     */
    @Override
    public T build()  {
        setUpAuthorization();

        super.executor(new ApacheHttpClient4Executor(prepareHttpClient(), this.clientRequestFilters))
             .clientErrorInterceptors(this.clientErrorInterceptors)
             .messageBodyProviders(this.jacksonJsonProvider, this.jacksonJsonProvider);

        return configureMetrics(super.build());

    }

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
     * oauth2 tokens. All you have to do is provide the client id and client secret via the appropriate constructor
     * and method.
     *
     * @param <T> the type of the client being built
     * @author chris.phillips
     */
    public static final class Builder<T> extends OpowerClient<T, Builder<T>> {

        static {
            HystrixPlugins.getInstance().registerEventNotifier(new OpowerEventNotifier());
        }

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
         */
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
         */
        public Builder(OpowerResourceInterface<T> resourceInterface, UriProvider uriProvider, String serviceName, String clientId) {
            super(resourceInterface, uriProvider, serviceName, clientId);
        }

        /**
         * Creates an OpowerClient.Builder instance that will use Zookeeper to look up server urls. This is
         * the preferred method of creating client instances.
         *
         * @param resourceInterface the resource interface
         * @param serviceDiscovery  The curator ServiceDiscovery instance that will be used to look up server urls
         * @param serviceName       The serviceName. This should match the name of the service as registered in Zookeeper
         * @param clientId          the oauth2 clientId. If you are not authorizing these service calls for some reason you must
         *                          still provide a clientId that can be used to identify the traffic this client will generate in
         *                          metrics and alerts.
         */
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
         */
        public Builder(OpowerResourceInterface resourceInterface, ServiceDiscovery<Void> serviceDiscovery,
                       String serviceName, String clientId) {
            super(resourceInterface, serviceDiscovery, serviceName, clientId);
        }

    }
}
