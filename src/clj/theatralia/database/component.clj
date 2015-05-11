(ns theatralia.database.component
  "Defines the component for taking care of the connection to the database.

  Also ensures that all data that are required for the system to operate are
  loaded into the database."
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [datomic.api :as d :refer [q db]]
            theatralia.readers)
  (:import datomic.Util))

;;; Credits:
;;;  - https://github.com/swannodette/om-async-tut/blob/master/src/leiningen/new/om_async_tut/server_util.clj

;;;; Utilities for data loading

(defn- transact-from-resource
  "Reads the data from the given resource and transacts them over the given
  connection. The resource should contain one or more vectors of transaction
  maps. Returns the future returned by the transaction."
  [resource conn]
  (->> resource
       io/resource
       io/reader
       Util/readAll
       (apply concat)
       (d/transact conn)))

(defn- load-if-empty
  "Loads data from resource if query returns empty result."
  [query resource conn]
  (when (empty? (q query (db conn)))
    (transact-from-resource resource conn)))

;;;; Queries that determine whether certain data has been loaded before

(def ^:private schema-query '[:find ?e :where [?e :db/ident :user/username]])

(def ^:private sample-data-query
  '[:find ?e :where [?e :material/title "On Food and Cooking"]])

;;;; The actual component

(defrecord Database [uri conn]
  component/Lifecycle
  (start [this]
    (d/create-database uri)
    (let [conn (d/connect uri)]
      (load-if-empty schema-query "database/schema.edn" conn)
      (load-if-empty sample-data-query "database/sample_data.edn" conn)
      (assoc this :conn conn)))
  (stop [this]
    (d/shutdown false) ; Don't want to stop the REPL, therefore false.
    (assoc this :conn nil)))

(defn make-database [uri]
  (component/using (map->Database {:uri uri})
                   [:logging])) ; Ensure proper start-up order. See
                                ; theatralia.logging.
