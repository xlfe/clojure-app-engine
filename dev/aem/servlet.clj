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


;;; This code is adapted from Ring (http://github.com/mmcgrana/ring).
;;;
;;; Required change from Ring: removed dependencies which use Java classes
;;; blacklisted in App Engine.


(ns appengine-magic.servlet
  (:use [appengine-magic.utils :only [copy-stream]])
  (:import [java.io File FileInputStream InputStream ByteArrayInputStream OutputStream]
           [javax.servlet.http HttpServlet HttpServletRequest HttpServletResponse]))


(defn- get-headers [^HttpServletRequest request]
  (reduce (fn [headers, ^String name]
            (assoc headers (.toLowerCase name) (.getHeader request name)))
          {}
          (enumeration-seq (.getHeaderNames request))))


(defn- make-request-map [^HttpServlet servlet
                         ^HttpServletRequest request
                         ^HttpServletResponse response]
  {:servlet            servlet
   :response           response
   :request            request
   :servlet-context    (.getServletContext servlet)
   :server-port        (.getServerPort request)
   :server-name        (.getServerName request)
   :remote-addr        (.getRemoteAddr request)
   :uri                (.getRequestURI request)
   :query-string       (.getQueryString request)
   :scheme             (keyword (.getScheme request))
   :request-method     (keyword (.toLowerCase (.getMethod request)))
   :headers            (get-headers request)
   :content-type       (.getContentType request)
   :content-length     (.getContentLength request)
   :character-encoding (.getCharacterEncoding request)
   :body               (.getInputStream request)})


(defn- set-response-headers [^HttpServletResponse response, headers]
  (doseq [[key val-or-vals] headers]
    (if (string? val-or-vals)
        (.setHeader response key val-or-vals)
        (doseq [val val-or-vals]
          (.addHeader response key val))))
  ;; Use specific servlet API methods for some headers:
  (.setCharacterEncoding response "UTF-8")
  (when-let [content-type (get headers "Content-Type")]
    (.setContentType response content-type)))


(defn- set-response-body [^HttpServletResponse response, body]
  (cond
   ;; just a string
   (string? body)
   (with-open [writer (.getWriter response)]
     (.print writer body))
   ;; any Clojure seq
   (seq? body)
   (with-open [writer (.getWriter response)]
     (doseq [chunk body]
       (.print writer (str chunk))
       (.flush writer)))
   ;; a Java InputStream
   (instance? InputStream body)
   (with-open [out (.getOutputStream response)
               ^InputStream b body]
     (copy-stream b out)
     (.flush out))
   ;; serve up a File
   (instance? File body)
   (let [^File f body]
     (with-open [stream (FileInputStream. f)]
       (set-response-body response stream)))
   ;; serve up a byte array
   (instance? (class (byte-array 0)) body)
   (with-open [in (ByteArrayInputStream. body)]
     (set-response-body response in))
   ;; nothing
   (nil? body) nil
   ;; unknown
   :else (throw (RuntimeException. (str "handler response body unknown" body)))))


(defn- adapt-servlet-response [^HttpServletResponse response,
                               {:keys [commit? status headers body]
                                :or {commit? true}}]
  (when commit?
    (if status
        (.setStatus response status)
        (throw (RuntimeException. "handler response status not set")))
    (when headers (set-response-headers response headers))
    (when body (set-response-body response body))))


(defn make-servlet-service-method [ring-handler]
  (fn [^HttpServlet servlet, ^HttpServletRequest request, ^HttpServletResponse response]
    (let [response-map (doall (ring-handler (make-request-map servlet request response)))]
      (when-not response-map
        (throw (RuntimeException. "handler returned nil (no response map)")))
      (adapt-servlet-response response response-map))))


(defn servlet [ring-handler]
  (proxy [HttpServlet] []
    (service [^HttpServletRequest request, ^HttpServletResponse response]
      ((make-servlet-service-method ring-handler) this request response))))
