(ns theatralia.database.t-txd-gen
  (:require [midje.sweet :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [theatralia.test.utils :refer [quick-check]]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [theatralia.database.canned-queries :as qcan]
            [theatralia.database.component :as db-component]
            [theatralia.database.txd-gen :as nut])) ; namespace under test

;;; Credits: http://yellerapp.com/posts/2014-05-07-testing-datomic.html

(defn empty-db-conn
  []
  (-> (db-component/make-database "datomic:mem://theatralia-test")
      component/start
      :conn))

(defn empty-db
  []
  (d/db (empty-db-conn)))

(defn suppose [db txd]
  (:db-after (d/with db txd)))

(defn contains-all? [haystack needles]
  (every? (set haystack) needles))

(facts "about add-tags-txd"
  (fact "After transacting the results, all tags should be present in DB."
    (let [db (empty-db)
          owner-eid (qcan/get-sandbox-eid db)]
      (prop/for-all [tags (gen/vector gen/string)]
        (let [db-after (suppose db ((nut/add-tags-txd db tags owner-eid) 1))
              tags-after (d/q '[:find [?t ...]
                                :in $ ?o
                                :where [?e :tag/text ?t]
                                       [?e :tag/owner ?o]]
                              db-after owner-eid)]
          (contains-all? tags-after tags))))
    => (quick-check 100))

  (fact "After transacting once, the result of the second run should be empty."
    (let [db (empty-db)
          owner-eid (qcan/get-sandbox-eid db)]
      (prop/for-all [tags (gen/vector gen/string)]
        (let [db-after (suppose db ((nut/add-tags-txd db tags owner-eid) 1))
              new-txd (nut/add-tags-txd db-after tags owner-eid)]
          (empty? (new-txd 1)))))
    => (quick-check 100))

  (fact "There should be no two tags with the same text and owner-eid."
    (let [db (empty-db)
          owner-eid (qcan/get-sandbox-eid db)]
      (prop/for-all [tags (gen/vector gen/string)]
        (let [db-after (suppose db ((nut/add-tags-txd db tags owner-eid) 1))
              tags-after (d/q '[:find [?t ...]
                                :in $ ?o
                                :where [?e :tag/text ?t]
                                [?e :tag/owner ?o]]
                              db-after owner-eid)]
          (apply distinct? tags-after))))
    => (quick-check 100)))
