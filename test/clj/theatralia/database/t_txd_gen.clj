(ns theatralia.database.t-txd-gen
  (:require [midje.sweet :refer :all]
            [clojure.data :refer [diff]]
            [clojure.pprint :refer [pprint]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [theatralia.test.utils :refer [quick-check]]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [theatralia.database.canned-queries :as qcan]
            [theatralia.database.component :as db-component]
            [theatralia.database.txd-gen :as txd-gen]))

;;;; General purpose helpers

(defn contains-all?
  "Returns true iff all elements from seq NEEDLES are contained in seq
  HAYSTACK."
  [haystack needles]
  (every? (set haystack) needles))

;;;; Auxiliary functions for testing against Datomic

;;; Credits: http://yellerapp.com/posts/2014-05-07-testing-datomic.html

(defn fresh-db
  "Returns a db that contains only the example data."
  []
  (-> (db-component/make-database "datomic:mem://theatralia-test")
      component/start
      :conn
      d/db))

(defn suppose
  "Returns a db that looks as if TXD had been transacted into DB."
  [db txd]
  (:db-after (d/with db txd)))

;;;; Sugar for easier testing of txd-gen/add-tags-txd

(defn add-tags-txd-only
  "Like txd-gen/add-tags-txd, but returns only the second element of the tuple
  returned by the add-tags-txd."
  [& args]
  ((apply txd-gen/add-tags-txd args) 1))

(defmacro tags-txd-prop
  "Template for shortening the definition of a property of txd-gen/add-tags-txd
  slighly. BODY will be evaluated in a context where ARGS are filled as follows:

    0 (tags)      – a generator generating a vector of strings to be used as
                    tags
    1 (owner-eid) – the eid of the user for whom we're adding tags
    2 (db-before) - the database value before adding TAGS
    3 (db-after)  – the database value after adding TAGS"
  [args & body]
  `(let [db-before# (fresh-db)
         owner-eid# (qcan/username->eid db-before# "sandbox")
         ~(subvec args 1 3) [owner-eid# db-before#]]
     (prop/for-all [tags# (gen/vector gen/string)]
       (let [db-after# (suppose db-before#
                                (add-tags-txd-only db-before# tags# owner-eid#))
             ~(args 0) tags#
             ~(args 3) db-after#]
         ~@body))))

;;;; Tests for txd-gen/add-tags-txd

(facts "about add-tags-txd"
  (fact "After transacting the results, all tags should be present in DB."
    (tags-txd-prop [tags owner-eid db-before db-after]
      (let [tags-after (qcan/tags-of-user-eid db-after owner-eid)]
        (contains-all? tags-after tags)))
    => (quick-check 100))

  (fact "Transacting the generated data doesn't modify pre-existing tags."
    (let [db-0 (fresh-db)
          owner-eid (qcan/username->eid db-0 "sandbox")]
      (prop/for-all [tags-1 (gen/vector gen/string)
                     tags-2 (gen/vector gen/string)]
        (let [db-1 (suppose db-0 (add-tags-txd-only db-0 tags-1 owner-eid))
              tag-eids-1 (d/q '[:find [?e ...] :where [?e :tag/text _]] db-1)
              db-2 (suppose db-1 (add-tags-txd-only db-1 tags-2 owner-eid))
              [tag-info-1 tag-info-2] (mapv #(d/pull-many % '[*] tag-eids-1)
                                            [db-1 db-2])]
          (= tag-info-1 tag-info-2))))
    => (quick-check 100))

  (fact "After transacting once, the result of the second run should be empty."
    (tags-txd-prop [tags owner-eid db-before db-after]
      (let [new-txd (add-tags-txd-only db-after tags owner-eid)]
        (empty? new-txd)))
    => (quick-check 100))

  (fact "There should be no two tags with the same text and owner-eid."
    (tags-txd-prop [tags owner-eid db-before db-after]
      (let [tags-after (qcan/tags-of-user-eid db-after owner-eid)]
        (apply distinct? tags-after)))
    => (quick-check 100)))
