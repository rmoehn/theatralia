(ns theatralia.db)

;; TODO: Get some structure into what we're doing by defining a Prismatic schema
;;       or something like that. See also
;;       https://github.com/Day8/re-frame/blob/master/examples/todomvc/src/todomvc/db.cljs.
;;       (RM 2015-08-27)

(def schema {:scratch/key {:db/unique :db.unique/identity}
             :search-result {:db/cardinality :db.cardinality/many}
             :tag/index {:db/unique :db.unique/identity}
