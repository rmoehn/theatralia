(ns theatralia.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [datascript :as d]
            [re-frame.core :as rf]
            [theatralia.db :as db]
            [theatralia.queries :as queries]
            [theatralia.thomsky :as tsky]
            [theatralia.utils :as th-utils :include-macros true]))

;;;; Simple forwarding subscription handlers

(rf/register-sub :kv-area/as-map
  (fn [conn [_ handle]]
    (reaction (db/kv-area-as-map @conn handle))))


;;;; Other subscription handlers

(defn search-result
  "Result of the material search."
  [db []]
  (tsky/bind '[:find [?rs ...]
               :where [_ :search-result ?rs]]
             db))
(th-utils/register-sub* search-result)

(defn tags
  [db []]
  (let [query-res (tsky/bind queries/tags-query db)]
    (reaction (sort-by first @query-res))))
(th-utils/register-sub* tags)
