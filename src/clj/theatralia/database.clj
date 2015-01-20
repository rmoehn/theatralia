(ns theatralia.database
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [datomic.api :as d :refer [q db]])
  (:import datomic.Util))

(defn- load-schema [schema-loc conn]
  (->> schema-loc
       io/resource
       io/reader
       Util/readAll
       (d/transact conn)))

(defrecord Database [uri conn]
  component/Lifecycle
  (start [this]
    (let [_    (d/create-database uri)
          conn (d/connect uri)]
      (load-schema "database/schema.edn" conn)
      (println "Loaded schema")
      (assoc this :conn conn)))
  (stop [this]
    (d/shutdown true)))

(defn make-database [uri]
  (component/using (map->Database {:uri uri})
                   []))
