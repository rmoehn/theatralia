(ns theatralia.web-server
  (:require [com.stuartsierra.component :as component]
            [theatralia.routes :refer [make-handler]]
            [ring.adapter.jetty :refer [run-jetty]]))

;;; Credits:
;;;  - http://youtu.be/13cmHf_kt-Q from around 31:00
;;;  - https://github.com/danielsz/system/blob/master/src/system/components/jetty.clj

(defrecord WebServer [port jetty web-app]
  component/Lifecycle
  (start [this]
    (assoc this
           :jetty
           (run-jetty (make-handler web-app) {:port port :join? false})))
  (stop [this]
    (when jetty
      (.stop jetty)
      (assoc this :jetty nil))))

(defn make-web-server [port]
  (component/using (map->WebServer {:port port})
                   [:web-app]))
