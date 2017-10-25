(ns cae.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]
           [com.google.appengine.api.datastore Query]
           [com.google.appengine.api.datastore FetchOptions$Builder]
           [com.google.appengine.api.datastore Entity])
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

(defroutes app
           (GET "/" [] (str "<h1>Hello World</h1><h2>db contains:</h2><p>" (read-db) "</p>"))
           (GET "/new/:name" [name] (write-db name))
           (route/not-found "<h1>Page not found.</h1>"))
