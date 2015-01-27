(ns theatralia.database
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [datomic.api :as d :refer [q db]])
  (:import datomic.Util))

(defn- no-schema-loaded? [conn]
  (empty? (q '[:find ?e :where [?e :db/ident :last-input/text]]
             (db conn))))

(defn- load-schema
  "Loads the Schema into the database unless it's loaded already. Blocks until
  the transaction has completed."
  [schema-loc conn]
  (when (no-schema-loaded? conn)
    (let [fut (->> schema-loc
                   io/resource
                   io/reader
                   Util/readAll
                   first
                   (d/transact conn))]
         @fut)))

(defrecord Database [uri conn]
  component/Lifecycle
  (start [this]
    (d/create-database uri)
    (let [conn (d/connect uri)]
      (load-schema "database/schema.edn" conn)
      (assoc this :conn conn)))
  (stop [this]
    (d/shutdown false) ; Don't want to stop the REPL, therefore false.
    (assoc this :conn nil)))

(defn make-database [uri]
  (component/using (map->Database {:uri uri})
                   []))
