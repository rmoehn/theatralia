(ns theatralia.test-runner
  (:require [cljs.test :as test :refer-macros [run-tests] :refer [report]]
            [theatralia.t-thomsky]))

;;; Credits: https://gitlab.com/keeds/cljsinit/blob/master/test/test-runner.cljs

(enable-console-print!)

(defmethod report [::test/default :summary] [m]
  (println "\nRan" (:test m) "tests containing"
           (+ (:pass m) (:fail m) (:error m)) "assertions.")
  (println (:fail m) "failures," (:error m) "errors.")
  (aset js/window "test-failures" (+ (:fail m) (:error m))))

(defn runner []
  (test/run-tests
    (test/empty-env ::test/default)
    'theatralia.t-thomsky))
