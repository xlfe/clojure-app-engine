(def appengine-version "1.9.58")

(defproject cae "0.1.0-SNAPSHOT"
  :dependencies [
                 [org.clojure/clojure "1.8.0"]
                 [ring "1.6.2"]
                 [com.google.appengine/appengine-api-1.0-sdk ~appengine-version]
                 [liberator "0.15.1"]
                 [compojure "1.6.0"]
                 ]
  :plugins [[lein-ring "0.12.1" :exclusions [org.clojure/clojure]]]
  :ring {:handler cae.core/prod-handler}
  :profiles {:dev
             {
              :source-paths ["dev/"]
              :dependencies
                            [[com.google.appengine/appengine-api-stubs ~appengine-version]
                             [com.google.appengine/appengine-local-runtime ~appengine-version]
                             [com.google.appengine/appengine-local-runtime-shared ~appengine-version]
                             ]
              }
             }
  )
