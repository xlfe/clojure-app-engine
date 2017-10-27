(ns cae.core
  (:use [liberator.core :only [resource defresource]])
  (:require [compojure.core :refer [defroutes ANY GET]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [compojure.route :as route])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]
           [com.google.appengine.api.datastore Query]
           [com.google.appengine.api.datastore FetchOptions$Builder]
           [com.google.appengine.api.datastore Entity]

           )
  )

(defn read-db
  []
  (let [datastore (DatastoreServiceFactory/getDatastoreService)
        query (Query. "item")
        prepared-query (.prepare datastore query)
        result (.asList prepared-query (FetchOptions$Builder/withDefaults))]
    (pr-str (map #(.getProperty %1 "name")
                 result))))

(defn write-db
  [name]
  (let [datastore (DatastoreServiceFactory/getDatastoreService)
        entity (Entity. "item")]
    (.setProperty entity "name" name)
    (let [key (.put datastore entity)]
      (str key))))

(defn tiger-not-found
  [ctx]
  (format "That is not THE right word: '%s'. Try again?" (get-in ctx [:request :params "word"])))

(defroutes app
           (ANY "/" [] (resource
                         :available-media-types ["text/html"]
                                 :handle-ok (fn [_]
                                              (str "<h1>Hello World</h1><h2>db contains:</h2><p>" (read-db) "</p>")
                                              )
                         )
                       )
           (GET "/new/:name" [name] (write-db name))
           (ANY "/secret" [] (resource :available-media-types ["text/html"]
                       :exists? (fn [ctx]
                                  (= "tiger" (get-in ctx [:request :params "word"])))
                       :handle-ok "You found the secret word!"
                       :handle-not-found tiger-not-found))
           (route/not-found "<h1>Page not found.</h1>"))

(def prod-handler
  (-> app
      wrap-params))

(def reload-handler
  (-> app
      wrap-reload
      wrap-params))
