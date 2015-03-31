(ns theatralia.main
  "Assembles systems for various uses. For development at the REPL we have
  (make-system). The other things are needed by the Leiningen Ring plugin for
  creating a WAR file that can be fed into Tomcat, for example. See the :ring
  section in project.clj.

  Note that at the moment I'm not sure anymore how all this works. I'll have to
  look into it again when I set up continuous integration and perhaps automatic
  deployment."
  (:require [com.stuartsierra.component :as component]
            [theatralia.database :refer [make-database]]
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

;; FIXME: (make-routes) doesn't return anything that contains a mapping for
;;        :handler.
(def production-handler
  (wrap-system (:handler (make-routes)) production-system))

(defn make-system [config]
  (component/system-map
    :web-app (make-routes)
    :web-server (make-web-server (:web-server-port config))
    :database (make-database (:database-uri config))))
