(ns theatralia.main
  (:require [com.stuartsierra.component :as component]
            [theatralia.web-server :refer [make-web-server]]))

(defn make-system [config]
  (component/system-map
    :web-server (make-web-server (:port config))))
