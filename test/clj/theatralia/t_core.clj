(ns theatralia.t-core
  (:require [midje.sweet :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [theatralia.test.utils :refer [quick-check]]
            [theatralia.core :refer [first-element]]))

(facts "about `first-element`"
  (fact "it normally returns the first element"
    (first-element [1 2 3] :default) => 1
    (first-element '(1 2 3) :default) => 1)

  (fact "it returns the first element for lots of vectors"
    (prop/for-all [fst gen/simple-type
                   v (gen/vector gen/simple-type)]
      (= fst (first-element (cons fst v) :default)))
      => (quick-check 100))

  ;; I'm a little unsure how Clojure types map onto the Lisp I'm used to.
  (fact "default value is returned for empty sequences"
    (first-element [] :default) => :default
    (first-element '() :default) => :default
    (first-element nil :default) => :default
    (first-element (filter even? [1 3 5]) :default) => :default))
