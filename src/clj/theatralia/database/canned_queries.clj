(ns theatralia.database.canned-queries
  "Some queries that are used in a number of places."
  (:require [datomic.api :as d]))

(defn get-sandbox-eid
  "Returns entity ID of user sandbox."
  [db]
  (d/q '[:find ?e . :where [?e :user/username "sandbox"]] db))
