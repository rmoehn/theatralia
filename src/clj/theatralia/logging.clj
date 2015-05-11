(ns theatralia.logging
  "Houses a component for setting up logging as we want it.

  Since the logging machinery sits in a global atom, the object returned by
  make-logging isn't supposed to be used directly by other components that
  depend on it. However, if another component X wants to log, it should declare
  its dependence on the logging component in order to ensure that logging is
  configured before X starts."
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]))

(defn make-logging [log-path]
  "Returns a component that activates Timbre logging to log-path on start and
  restores default logging configuration on stop."
  (reify
    component/Lifecycle
    (start [this]
      (timbre/set-config! [:shared-appender-config :spit-filename] log-path)
      (timbre/set-config! [:appenders :spit :enabled?] true))
    (stop [this]
      (swap! timbre/config (constantly timbre/example-config)))))
        ; Maybe not the nicest way, but timbre doesn't provide for a better one.
