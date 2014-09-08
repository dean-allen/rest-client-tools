1.0.9
-----
- chris.phillips open sourced rest-client-generator, rest-client-test, rest-client-hystrix.
- chris.phillips add rest-params to list of valid dependencies in rest-interface-base
- geoff.the rework clientSecret method in OpowerClient.Builder

1.0.8
-----
- benson.fung fixed http status code for UnauthorizedRequestException
- geoff.the add service name to exception to aid in debugging
- chris.phillips fix rest-params to match behavior of BaseParam from archmage
- chris.phillips ported builtin MessageBodyReader/Writers from resteasy
- chris.phillips fixed behavior of Response.getEntity()
- chris.phillips coordinate timeout with retry settings. new default is 7s

1.0.7
-----
- chris.phillips fixed constructor on IntervalParam

1.0.6
-----
- chris.phillips added rest-params module
- geoff.the updated README with exception mappings

1.0.5
-----
- chris.phillips added RoundRobinUriProvider
- chris.phillips allow packages that start with opower.*

1.0.4
-----
- chris.phillips fixed logic for configuring sensu publishing
- chris.phillips Coordinated size of httpclient pool with hystrix thread pool sizes
- matt.greenfield updated to latest opower-parent

1.0.3
-----
- chris.phillips updated to latest version of metrics-providers
- sachin.nene fixed how parameters of type com.google.common.base.Optional<?> are handled

1.0.2
-----
- chris.phillips upgraded to metrics-providers 1.0.0
