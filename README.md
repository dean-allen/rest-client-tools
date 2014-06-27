rest-client-tools
=================

[![Build Status](http://jenkins-dev.va.opower.it/job/rest-client-tools/badge/icon)](http://jenkins-dev.va.opower.it/job/rest-client-tools/)

Tools for generating JAX-RS proxies.

First thing you'll want to do is clone this so you can explore. I purposefully didn't create this as a PR to try and discourage
an avalanche of nitpicking. The feedback needed at this stage is at a pretty high level. So please constrain your feedback to high
level design concerns / suggestions. I'd also like to solicit PRs from you for important changes and unit tests that you'd like to see. 
If you take the time to contribute code don't worry about cleaning it all up. I'll make sure to do that later when I go to move 
this into its more official home. I'm going to put a README.md inside each of the projects with some of the notes about changes 
I made as I moved things around. What happened was I started pulling pieces out of archmage-java-client and auth-core and 
archmage-metrics and discovered a bunch of things that could be massaged to make the code nice and less tangly.


Here's an overview that I hope helps you get started.

The main entry point that people would use for these tools is the ClientBuilder.

I've created a diagram of the various ClientBuilders provided here: https://wiki.opower.com/display/~chris.phillips/New+client+bits
The first two, ClientBuilder and HystrixClientBuilder are designed to be very generic and have no Opower specific dependencies.
The OpowerClientBuilder uses the HystrixClientBuilder and provides sensible Opower specific defaults.

Here are some high level notes about the important pieces:

ResourceClass - 
This class wraps the interface for the JAX-RS Resource and provides validation to make sure that the class used is an interface
and has been annotated with JAX-RS annotations etc. There is a OpowerResourceClass which provides additional validation to 
constrain the classes used on the interface to those we have blessed.

UriProvider -
Implementations of this interface are responsible for providing the base URI for each request made by the generated proxies.

ClientExecutor - 
Abstraction over http transport libraries. Allows us to choose between say httpclient or netty or some other http request library.
Allows for processing a List<ClientRequestFilter> as well.

ClientRequestFilter -
Implementations of this interface can modify and otherwise prepare the request before it is sent to the server.

ClientBuilder -
The basis for client proxy generation. Given a ResourceClass and a UriProvider, it generates a proxy
 that will make the appropriate http requests for consuming the REST API specified by the ResourceClass.
 
MessageBodyReader/Writer -
Responsible for translation the to/from Java objects to the wire format required by the service. In the OpowerClientBuilder case
we use JacksonJsonProvider for both of these.

ErrorInterceptor -
These are standar JAX-RS ErrorInterceptors used for handling errors from service calls.

HystrixClientBuilder -
Adds to the basic ClientBuilder by making each http call inside a HystrixCommand. By default, all the commands will use the
same CommandGroupKey based on the class name of the provided ResourceClass

Here is a diagram of the modules and their hierarchy:

https://wiki.opower.com/display/~chris.phillips/rest-client-tools+module+hierarchy

Random notes:

No dependencies on anything archmage to be found here.

Uses straight slf4j api for logging.

Some of these changes imply somewhat significatn changes to Archmage like relocating classes to separate libs, removing some things
entirely, and also some refactoring (eg CuratorServiceRegistry). Some thought needs to be given to backwards compatibility and
migration. I'm hoping that in certain cases some people could just start using the new client tools with no problems. Others will
have to worry about what archmage they're on which is kinda hairy. But I think we can manage this transition.
