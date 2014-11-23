(ns theatralia.main
  (:require [com.stuartsierra.component :as component]
            [theatralia.web-server :refer [make-web-server]]
            [theatralia.routes :refer [make-routes]]))

;;; Credits:
;;;  - https://github.com/stuartsierra/component

(defn make-system [config]
  (component/system-map
    :web-app (make-routes)
    :web-server (make-web-server (:port config))))
