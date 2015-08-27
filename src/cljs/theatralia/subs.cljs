(ns theatralia.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [theatralia.thomsky :as tsky]
            [theatralia.utils :as th-utils :include-macros true]))

(defn get-scratch-entid
  "Entity ID of the scratch area with SCRATCH-KEY as the value of its
  :scratch/key attribute. Assumes that this scratch area already exists."
  [conn [_ scratch-key]]
  (let [r (tsky/bind '[:find ?e .
                       :in $ ?key
                       :where [?e :scratch/key ?key]]
                     conn scratch-key)]
    (assert @r)
    r))
(th-utils/register-sub* get-scratch-entid)

(defn get-scratch-contents
  "Contents of scratch area with entity ID SCRATCH-ENTID."
  [conn [_ scratch-entid]]
  (reaction (d/pull @conn '[*] scratch-entid)))
(th-utils/register-sub* get-scratch-contents)

(defn search-result
  "Result of the material search."
  [db []]
  (tsky/bind '[:find [?rs ...]
               :where [_ :search-result ?rs]]
             db))
(th-utils/register-sub* search-result)

(defn tags
  [db []]
  (let [query-res (tsky/bind tags-query db)]
    (reaction (sort-by first @query-res))))
(th-utils/register-sub* tags)
