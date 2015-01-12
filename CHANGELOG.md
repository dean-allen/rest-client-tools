1.1.0
-----
- added @ResourceMetadata annotation to make service writers instead of service consumers responsible for specifying serviceName and modelPackages
- fixed bug that required specifying a separate UriProvider when using auth

1.0.10
------
- upgraded to auth-common 1.0.1

1.0.9
-----
- open sourced rest-client-generator, rest-client-test, rest-client-hystrix.
- add rest-params to list of valid dependencies in rest-interface-base
- rework clientSecret method in OpowerClient.Builder

1.0.8
-----
- fixed http status code for UnauthorizedRequestException
- add service name to exception to aid in debugging
- fix rest-params to match behavior of BaseParam from archmage
- ported builtin MessageBodyReader/Writers from resteasy
- fixed behavior of Response.getEntity()
- coordinate timeout with retry settings - new default is 7s

1.0.7
-----
- fixed constructor on IntervalParam

1.0.6
-----
- added rest-params module
- updated README with exception mappings

1.0.5
-----
- added RoundRobinUriProvider
- allow packages that start with opower.*

1.0.4
-----
- fixed logic for configuring sensu publishing
- coordinated size of httpclient pool with hystrix thread pool sizes
- updated to latest opower-parent

1.0.3
-----
- updated to latest version of metrics-providers
- fixed how parameters of type com.google.common.base.Optional<?> are handled

1.0.2
-----
- upgraded to metrics-providers 1.0.0
