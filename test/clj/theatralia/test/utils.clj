(ns theatralia.test.utils
  "Small utilities for making testing easier."
  (:require [midje.sweet :refer [just anything]]
            [clojure.test.check :as tc]))

;;; Credits: http://zotskolf.nl/2014/11/10/testcheckbasics.html
(defn quick-check [times]
  "Checks that the given prop holds for times runs with
  clojure.test.check/quick-check."
  (fn [prop]
    ((just {:result true :num-tests times :seed anything})
     (tc/quick-check times prop))))
