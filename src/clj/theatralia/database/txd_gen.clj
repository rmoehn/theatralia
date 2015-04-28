(ns theatralia.database.txd-gen
  "Functions for generating Datomic transaction data structures."
  (:require [datomic.api :as d]
            [medley.core :refer [remove-vals]]))

;;;; Utilities for adding materials

(defn invert [s]
  (map - s))

(defn dezip [s]
  (let [tuple-size (count (first s))]
    (mapv (fn [n]
            (map #(nth % n) s))
          (range tuple-size))))

;;;; Generators for adding materials

(defn get-present-tags [db tags owner-eid]
  (let [tags-s (set tags)]
    (dezip (d/q '[:find ?e ?t
                  :in $ ?ts ?o
                  :where [?e :tag/owner ?o]
                         [?e :tag/text ?t]
                         [(contains? ?ts ?t)]]
                db tags owner-eid))))

(defn add-tags-txd [db tags owner-eid]
  "Generates a transaction data structure for adding tags for user with
  owner-eid to db.

  Assumes that owner-eid is already in database or being added during the same
  transaction.

  Returns a seq of the (temporary) entity IDs to be used in the same transaction
  and a vector of transaction maps."
  (when tags
    (let [[present-tags eids] (get-present-tags db tags owner-eid)
          tags-to-add (remove (set present-tags) tags)
          tempids (map #(d/tempid :part/bibliography)
                       (invert (range (count tags-to-add))))]
      [(concat eids tempids)
       (mapv (fn [i t]
               {:db/id i
                :tag/text t
                :tag/owner owner-eid})
             tempids tags-to-add)])))

(defn add-material-txd [m tag-eids owner-eid]
  "Generates a transaction data structure for adding a material with title, URI
  and comments from m, tags with entity IDs from tag-eids and owner with
  owner-eid.

  Assumes that tags with tag-eids and owner with owner-eid are already in the
  database or being added during the same transaction."
  (remove-vals nil? {:db/id #db/id[:part/bibliography]
                     :material/title    (m :title)
                     :material/uri      (m :uri)
                     :material/comments (m :comments)
                     :material/tags     tag-eids
                     :material/owner    owner-eid}))
