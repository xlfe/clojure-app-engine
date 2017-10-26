# Clojure on Google AppEngine

A lein skeleton to get started with Clojure on Google App Engine

Based on a couple of very helpful blogs:

* https://blog.jeaye.com/2016/08/23/clojure-app-engine/
* http://lambda-startup.com/developing-clojure-on-app-engine/
* http://flowa.fi/blog/2014/04/25/clojure-gae-howto.html

I found a few inconsistencies from the above blogs.

1. Appstats is not supported on Java 7 (this skeleton uses Java 8). Stackdriver Trace (Appstats replacement) works out of the box instead.
2. I didn't need to use lein-localrepo to get the GAE Local Deveserver running (as suggested by the lambda-startup post).

### Examples

The skeleton includes two routes (defined in [core.clj](src/cae/core.clj)) which write and read to the datastore

### Getting up and running with a local development server

This has been tested on App Engine 1.9.58

Start a repl by running  `lein repl` then boot the development appserver (see [appengine.clj](dev/cae/appengine.clj))

```clojure
(require '[cae.appengine :as ae])
(ae/start-it)
```

### Deploying to Appengine

Make sure you have put your Google App Engine project name into [appengine-web.xml](appengine-web.xml), then:

```bash
bash deploy_to_appengine.sh
```
