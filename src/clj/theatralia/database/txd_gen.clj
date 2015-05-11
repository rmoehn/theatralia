(ns theatralia.database.txd-gen
  "Functions for generating Datomic transaction data structures."
  (:require [datomic.api :as d]
            [medley.core :refer [remove-vals]]))

;;;; Utilities for adding materials

(defn invert [s]
  "Maps a sequence of numbers to a sequence of those numbers each multiplied
  with -1."
  (map - s))

(defn dezip [s]
  "Something like the inverse of zip. – Unravels a seq of m n-tuples into a
  n-tuple of seqs of length m.

  Example:
    (dezip '([11 12] [21 22] [31 32] [41 42]))
      ;=> [(11 21 31 41) (12 22 32 42)]

  Umm, actually there is no zip in Clojure. Instead, you'd use this:
    (apply map vector ['(11 21 31 41) '(12 22 32 42)])
      ;=> ([11 12] [21 22] [31 32] [41 42]))

  Note that I'm using lists here only for demonstrating that we're not limited
  to vectors."
  (let [tuple-size (count (first s))
        s-seq (seq s)]
    (mapv (fn [n]
            (map #(nth % n) s-seq))
          (range tuple-size))))

;;;; Generators for adding materials

(defn get-present-tags [db tags owner-eid]
  "Returns a tuple [eids tag-texts] for all tags of user with OWNER-ID that are
  both in the database and in TAGS."
  (dezip (d/q '[:find ?e ?t
                :in $ ?ts ?o
                :where [?e :tag/owner ?o]
                       [?e :tag/text ?t]
                       [(contains? ?ts ?t)]]
              db (set tags) owner-eid)))

(defn add-tags-txd [db tags user-eid]
  "Generates a transaction data structure for adding tags for user with
  user-eid to db.

  Assumes that user with user-eid is already in database or being added during
  the same transaction.

  Returns a seq of the (temporary) entity IDs to be used in the same transaction
  and a vector of transaction maps."
  (when tags
    (let [[eids present-tags] (get-present-tags db tags user-eid)
          tags-to-add (remove (set present-tags) tags)
          tempids (map #(d/tempid :part/bibliography %)
                       (invert (range (count tags-to-add))))]
      [(concat eids tempids)
       (mapv (fn [i t]
               {:db/id i
                :tag/text t
                :tag/owner user-eid})
             tempids tags-to-add)])))

(defn add-material-txd [m tag-eids user-eid]
  "Generates a transaction data structure for adding a material with title, URI
  and comments from m, tags with entity IDs from tag-eids and owner with
  user-eid.

  Assumes that tags with tag-eids and user with user are already in the
  database or being added during the same transaction."
  (remove-vals nil? {:db/id #db/id[:part/bibliography]
                     :material/title    (m :title)
                     :material/uri      (m :uri)
                     :material/comments (m :comments)
                     :material/tags     tag-eids
                     :material/owner    user-eid}))
