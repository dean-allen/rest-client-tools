Extension to ClientBuilder. By default it will execute each http request inside of a HystrixCommand. The CommandGroupKey 
is by default the same for all methods on the ResourceClass interface. However, per method configuration is available
for people that need different properties for different methods or perhaps separate thread pools etc. This hides a lot of the
features of Hystrix (callbacks, collapsing are the two most notable), but it gives us the main value we seek from Hystrix--
circuit breaker hotness. I think there's a way where we can take the bits assembled in this rest-client-tools project and
create something similar to the old BasicClient that will expose more of the fun things about Hystrix to users. But I didn't
go down that road yet since getting all this going first was more important.