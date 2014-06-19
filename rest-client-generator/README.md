This is the same rest-client-proxy that was the code adapted from RestEasy. I tweaked a few things here and there. I added
a few more example ClientExecutors for fun. I think it'd be cool to use async-http-client or the latest version of httpclient.
I like them better. I also added in the ClientRequestFilter concept since it feels more at home at this level rather than
higher up linked to a specific http library.

