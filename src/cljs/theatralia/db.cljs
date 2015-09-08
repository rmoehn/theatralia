(ns theatralia.db
  "Definitions of the schema of the DataScript DB in the client."
  (:require [datascript :as d]
            [plumbing.core :as plumbing :refer [safe-get]]
            [schema.core :as s]))

;;;; Database helper functions

(defn kv-area-as-map
  "Retrieves the contents of the key-value store area identified by HANDLE from
  DB and returns them as a key-value map."
  [db handle]
  (let [cell-maps (-> (d/pull db
                              '[{:kv-area/cells [:kv-cell/key :kv-cell/val]}]
                              [:kv-area/handle handle])
                      (get :kv-area/cells))]
    (plumbing/for-map [{k :kv-cell/key v :kv-cell/val} cell-maps]
      k v)))


;;;; Transaction functions

;;; These functions are thought to be called within transactions with
;;; :db.fn/call. They return transaction data.

(defn retract-entities
  "Retracts entities according to query Q.

  Q with arguments Q-ARGS has to return entity IDs when applied to DB. Returns
  transaction data that will retract all the entities with those entity IDs from
  DB."
  [db q & q-args]
  (let [eids (apply d/q q db q-args)]
    (map (fn [eid] [:db.fn/retractEntity eid]) eids)))


;;;; Schema

(def full-schema
  "Complete schema for the DB.

  DataScript only supports a subset of schema attributes, so we can't use this
  verbatim. Especially, it doesn't support custom value types specified with
  Prismatic schema. So this is mainly for documentation."
  {:kv-area/handle {:db/unique :db.unique/identity
                    :db/valueType cljs.core/UUID}
   :kv-area/cells {:db/cardinality :db.cardinality/many
                   :db/valueType   :db.type/ref}

   :kv-cell/key {:db/valueType :db.type/keyword}
   :kv-cell/val {:db/valueType s/Any}

   :search/result
   {:db/cardinality :db.cardinality/many
    :db/valueType [(s/one s/Int "server entity ID")
                   (s/one s/Str "result title")
                   (s/one s/Num "score")]}

   :tag/s-id {:db/unique :db.unique/identity
              :db/valueType :db.type/long}
   :tag/text {:db/valueType :db.type/string}})

(def schema
  "Actual schema for the DataScript database, generated from the full schema.

  This is all DataScript supports."
  (plumbing/for-map [[a m] full-schema]
    a (plumbing/for-map [[aa v] m
                         :when (or (= aa :db/cardinality)
                                   (= aa :db/unique)
                                   (= v :db.type/ref))]
        aa v)))
