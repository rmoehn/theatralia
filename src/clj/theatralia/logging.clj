(ns theatralia.logging
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]))

(defn make-logging [log-path]
  (reify
    component/Lifecycle
    (start [this]
      (timbre/set-config! [:shared-appender-config :spit-filename] log-path)
      (timbre/set-config! [:appenders :spit :enabled?] true))
    (stop [this]
      (swap! timbre/config (constantly timbre/example-config)))))
        ; Maybe not the nicest way, but timbre doesn't provide for a better one.
