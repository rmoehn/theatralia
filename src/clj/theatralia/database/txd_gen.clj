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

(defn get-present-tags [conn tags owner-eid]
  (let [tags-s (set tags)]
    (dezip (d/q '[:find ?e ?t
                  :in $ ?ts ?o
                  :where [?e :tag/owner ?o]
                         [?e :tag/text ?t]
                         [(contains? ?ts ?t)]]
                (d/db conn) tags owner-eid))))

(defn add-tags-txd [conn tags owner-eid]
  (when tags
    (let [[present-tags eids] (get-present-tags conn tags owner-eid)
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
  (remove-vals nil? {:db/id #db/id[:part/bibliography]
                     :material/title    (m :title)
                     :material/uri      (m :uri)
                     :material/comments (m :comments)
                     :material/tags     tag-eids
                     :material/owner    owner-eid}))
