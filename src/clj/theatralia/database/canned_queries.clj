(ns theatralia.database.canned-queries
  "Some queries that are used in a number of places."
  (:require [datomic.api :as d]))

(defn username->eid
  "Returns entity ID of user with username."
  [db username]
  (d/q '[:find ?e . :in $ ?un :where [?e :user/username ?un]]
       db username))

(defn tags-of-user-eid [db eid]
  "Returns all tags whose owner has eid."
  (d/q '[:find [?tt ...]
         :in $ ?o
         :where [?t :tag/owner ?o]
                [?t :tag/text ?tt]]
       db eid))

(defn tags-of-user
  "Returns all tags whose owner has username."
  [db username]
  (d/q '[:find [?tt ...]
         :in $ ?un
         :where [?o :user/username ?un]
                [?t :tag/owner ?o]
                [?t :tag/text ?tt]]
       db username))
