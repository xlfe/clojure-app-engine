(ns cae.appengine
  (:require [cae.core :as handler])
  (:use ring.adapter.jetty))

(def myport 8080)
(def myserver (atom nil))

(defmacro with-app-engine
  "testing macro to create an environment for a thread"
  ([body]
   `(with-app-engine env-proxy ~body))
  ([proxy body]
   `(last (doall [(com.google.apphosting.api.ApiProxy/setEnvironmentForCurrentThread ~proxy)
                  ~body]))))

(defn login-aware-proxy
  "returns a proxy for the google apps environment that works locally"
  [request]
  (let [email (:email (:session request))]
    (proxy [com.google.apphosting.api.ApiProxy$Environment] []
      (isLoggedIn [] (boolean email))
      (getAuthDomain [] "")
      (getRequestNamespace [] "")
      (getDefaultNamespace [] "")
      (getAttributes [] (java.util.HashMap.))
      (getEmail [] (or email ""))
      (isAdmin [] true)
      (getAppId [] "local"))))

(defn environment-decorator
  "decorates the given application with a local version of the app engine environment"
  [application]
  (fn [request]
    (with-app-engine (login-aware-proxy request)
                     (application request))))

(defn init-app-engine
  "Initialize the app engine services."
  ([]
   (init-app-engine "."))
  ([dir]
   (let [factory (com.google.appengine.tools.development.ApiProxyLocalFactory.)
         env (reify com.google.appengine.tools.development.LocalServerEnvironment
               (getAppDir [this] (java.io.File. dir))
               (getAddress [this] "localhost")
               (getHostName [this] "localhost")
               (getPort [this] myport)
               (waitForServerToStart [this])
               (enforceApiDeadlines [this] true)
               (simulateProductionLatencies [this] true))
         delegate (.create factory env)]
     (com.google.apphosting.api.ApiProxy/setDelegate delegate))))

(defn start-it []
  (if (not @myserver)
    (init-app-engine))
  (reset! myserver
          (run-jetty (environment-decorator handler/app)
                     {:port myport :join? false}))
  (.start @myserver))

(defn stop-it []
  (.stop @myserver)
  (reset! myserver nil))