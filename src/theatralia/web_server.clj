(ns theatralia.web-server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :as cj]
            [compojure.route :as route]))

;;; Credits:
;;;  - https://github.com/weavejester/compojure/
;;;  - http://youtu.be/13cmHf_kt-Q from around 31:00
;;;  - https://github.com/danielsz/system/blob/master/src/system/components/jetty.clj

(cj/defroutes routes
  (cj/GET "/" [] "<h1>Hello World</h1>")
  (route/not-found "<h1>Page not found</h1>"))

(defrecord WebServer [port jetty]
  component/Lifecycle
  (start [this]
    (assoc this
           :jetty
           (run-jetty routes {:port port :join? false})))
  (stop [this]
    (when jetty
      (.stop jetty))))

(defn make-web-server [port]
  (component/using (map->WebServer {:port port}) []))
