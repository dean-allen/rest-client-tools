I did some massaging of the filter stuff that Matt did. I pushed the filter concept down into rest-client-proxy so that it 
would interoperate better with the ClientExecutor concept there. Then we could switch off of apache-http-client if need be.
I also made the RequestId thing into a filter. 

The pattern I went for with the OpowerClientBuilder was to have defaults and provide callbacks for big complicated things
like the ObjectMapper and JacksonJsonProvider. I thought that played well in the old BasicClient. I've also started liking
this pattern of having Optional<?> for things that are optional and then doing a Optional.fromNullable in the method.

Everything has a sensible default and I think we have all the right hooks for further customization.


One important thing here is the stuff I did regarding Curator. I feel like CuratorServiceRegistry is trying to do too much
and that the lazy loading of it is not ideal either. So I pulled the bits out of it for building up a url and put that into
the CuratorUriProvider. Check that guy out. That is why you have to provide a ServiceDiscovery instance to the 
OpowerClientBuilder constructor. I think we need to make it easy to access a singleton ServiceDiscovery instance from inside archamge
that corresponds to a singleton CuratorFramework instance. Then the CuratorServiceRegistry can take in its constructor an
already started CuratorFramework instance and focus only on the register/ unregister part. I think that will make us happier
in the long run.


#### Mappings provided by ExceptionMapperInterceptor :

| org.apache.http.HttpStatus | Numeric | com.opower.rest.exception class |
|:---------------------------|:--------|:----------|
| SC_BAD_REQUEST | 400 | BadRequestException |
| SC_FORBIDDEN   | 403 | ForbiddenException |
| SC_NOT_FOUND   | 404 | NotFoundException |
| SC_GONE        | 410 | GoneException |
| SC_INTERNAL_SERVER_ERROR | 500 | InternalServerErrorException |
| SC_SERVICE_UNAVAILABLE | 503 | ServiceUnavailableException |
| SC_UNAUTHORIZED | 401 | UnauthorizedRequestException |

