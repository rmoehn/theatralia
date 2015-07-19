(ns theatralia.t-thomsky
  (:require [cljs.test :refer-macros [deftest is are run-tests]]
            [theatralia.thomsky :as tsky]))

;;; Credits: @#' swearing in order to access private vars is recommended here:
;;; http://dev.clojure.org/display/community/Library+Coding+Standards
;;; Be aware of
;;; https://groups.google.com/d/topic/clojure/Mi277rszUs0/discussion, too, if
;;; you intend to use this in other places.

(deftest test-single-valued?
  (are [b q] (= b (@#'tsky/single-valued? q))
       true '[:find ?e . :where [?e :attr _]]))
