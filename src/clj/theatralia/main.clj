(ns theatralia.main
  (:require [com.stuartsierra.component :as component]
;            [theatralia.database :refer [make-database]]
            [theatralia.web-server :refer [make-web-server]]
            [theatralia.routes :refer [make-routes]]))

;;; Credits:
;;;  - http://youtu.be/13cmHf_kt-Q from around 31:00
;;;  - https://github.com/stuartsierra/component
;;;  - http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded

(def production-system)

(defn make-production-system []
  (component/system-map))

(defn start-production-system []
  (alter-var-root
    #'production-system
    (constantly (component/start (make-production-system)))))

(defn stop-production-system []
  (alter-var-root production-system component/stop))

(defn wrap-system [handler system]
  (fn [req]
    (handler (assoc req ::system system))))

(def production-handler
  (wrap-system (:handler (make-routes)) production-system))

(defn make-system [config]
  (component/system-map
    :web-app (make-routes)
    :web-server (make-web-server (:web-server-port config))
    #_:database #_(make-database (:database-uri config))))
