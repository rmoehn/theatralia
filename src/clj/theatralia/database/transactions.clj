(ns theatralia.database.transactions
  "Functions providing the complete write interface to our database."
  (:require [slingshot.slingshot :refer [throw+]]
            [dire.core :refer [with-handler]]
            [taoensso.timbre :as timbre]
            [datomic.api :as d]
            [theatralia.database.canned-queries :as qcan]
            [theatralia.database.txd-gen :as txd-gen])
  (:import java.util.concurrent.ExecutionException))

(defn add-material
  "Add a material m to the collection of user with username.

  Note that we're not putting the not-yet-existing tags into the database with
  one transaction and then putting the material with a second transaction. That
  would be easier to program, but we wouldn't have the atomicity benefit of
  transactions: either both material and tags are added or none. (This might be
  obvious, but, revisiting this code, I was puzzled for a moment why I was doing
  such gymnastics with generating transaction data."
  [conn m username]
  (let [db (d/db conn)
        user-eid (qcan/username->eid username)
        [tag-eids tags-txd] (txd-gen/add-tags-txd db  (m :tags) user-eid)
        mat-txd  (txd-gen/add-material-txd db m tag-eids user-eid)]
    @(d/transact conn (conj tags-txd mat-txd))))

(with-handler #'add-material
  ExecutionException
  (fn [e & args]
    (timbre/error (.getCause e))
    (throw+ {:type :could-not-add})))
