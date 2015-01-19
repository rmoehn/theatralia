(ns theatralia.database
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d :refer [q db]]))

(defrecord Database [uri conn]
  component/Lifecycle
  (start [this]
    (do
      (d/create-database uri)
      (assoc this
             :conn
             (d/connect uri))))
  (stop [this]
    (d/shutdown true)))

(defn make-database [uri]
  (component/using (map->Database {:uri uri})
                   []))
