; modified from https://github.com/gcv/appengine-magic under MIT License
;
; Copyright (c) 2010 Constantine Vetoshev
;
;Permission is hereby granted, free of charge, to any person obtaining a copy of
;this software and associated documentation files (the "Software"), to deal in
;the Software without restriction, including without limitation the rights to
;use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
;the Software, and to permit persons to whom the Software is furnished to do so,
;subject to the following conditions:
;
;The above copyright notice and this permission notice shall be included in all
;copies or substantial portions of the Software.
;
;THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
;FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
;COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
;IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
;CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


(ns appengine-magic.jetty
  (:use [appengine-magic.servlet :only [servlet]])
  (:import org.mortbay.jetty.handler.ContextHandlerCollection
           [org.mortbay.jetty Server Handler]
           javax.servlet.http.HttpServlet
           javax.servlet.Filter
           [org.mortbay.jetty.servlet Context ServletHolder FilterHolder]))


(defn- proxy-multihandler
  "Returns a Jetty Handler implementation for the given map of relative URLs to
   handlers. Each handler may be a Ring handler or an HttpServlet instance."
  [filters all-handlers]
  (let [all-contexts (ContextHandlerCollection.)
        context (Context. all-contexts "/" Context/SESSIONS)]
    (doseq [[url filter-objs] filters]
      (let [filter-objs (if (sequential? filter-objs) filter-objs [filter-objs])]
        (doseq [filter-obj filter-objs]
          (.addFilter context (FilterHolder. filter-obj) url Handler/ALL))))
    (doseq [[relative-url url-handler] all-handlers]
      (.addServlet context (ServletHolder. url-handler) relative-url))
    all-contexts))


(defn #^Server start [filter-map servlet-map &
                      {:keys [port join?] :or {port 8080 join? false}}]
  (let [server (Server. port)]
    (doto server
      (.setHandler (proxy-multihandler filter-map servlet-map))
      (.start))
    (when join? (.join server))
    server))


(defn stop [#^Server server]
  (.stop server))
