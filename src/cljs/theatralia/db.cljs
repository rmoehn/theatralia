(ns theatralia.db
  "Definitions of the schema of the DataScript DB in the client."
  (:require [plumbing.core :as plumbing]
            [schema.core :as s]))

(def full-schema
  "Complete schema for the DB.

  DataScript only supports a subset of schema attributes, so we can't use this
  verbatim. Especially, it doesn't support custom value types specified with
  Prismatic schema. So this is mainly for documentation."
  {:scratch/handle {:db/unique :db.unique/identity}
   :scratch/kv-cells {:db/cardinality :db.cardinality/many
                      :db/valueType   :db.type/ref}

   :kv-cell/key {:db/valueType :db.type/string}
   :kv-cell/val {:db/valueType s/Any}

   :search/result
   {:db/cardinality :db.cardinality/many
    :db/valueType [(s/one s/Int "server entity ID")
                   (s/one s/String "result title")
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
