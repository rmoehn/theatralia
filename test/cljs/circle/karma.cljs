(ns circle.karma
  (:require [clojure.string :as string]
            [cljs.test :as test :include-macros true]))

(defn get-total-test-count []
  (reduce + (map count (vals @test/registered-tests))))

;; A report function to override the default cljs-test one.
(defmulti report :type)

(defmethod report :begin-test-var [{:keys [test-env]}]
  ;; Set the current timer.
  (swap! test-env assoc ::test-start (.getTime (js/Date.)))
  ;; Collect the output.
  (swap! test-env assoc ::test-output [])
  (swap! test-env assoc ::old-print-fn *print-fn*)
  (test/set-print-fn! (fn [output] (swap! test-env update-in [::test-output] #(conj %1 output)))))

(defmethod report :end-test-var [{:keys [test-env] :as report}]
  (let [;; Get the start time from the test env, and the end time is now.
        start (-> @test-env ::test-start )
        end (.getTime (js/Date.))
        ;; Collect the output from the test env.
        output (->> @test-env ::test-output (string/join "\n"))]
    (test/set-print-fn! (-> @test-env ::old-print-fn))
    ;; Clean up the test env.
    (swap! test-env dissoc ::test-start ::test-output ::old-print-fn)
    ;; Report results to karma.
    (.result js/__karma__
             (clj->js { "id" ""
                        "description" (-> report :test-name str)
                        "suite" [(-> report :test-name namespace str)]
                        "success" (and (zero? (:error @test-env))
                                       (zero? (:fail @test-env)))
                        "skipped" nil
                        "time" (- end start)
                        "log" [output]}))))

;; Make beginning a namespace's tests less noisy.
(defmethod report :begin-test-ns [_])

;; Fall back to the default report function so that e.g. errors are logged correctly.
(let [cljs-test-report test/report]
  (defmethod report :default [data]
    (cljs-test-report data)))


(defn ^:export run-tests-for-karma []
  (.info js/__karma__ (clj->js {:total (get-total-test-count)}))
  (doseq [[nmsp ns-tests] @test/registered-tests] ; <- This atom doesn't exist in cljs.test.
    (with-redefs [test/report report]
      (test/test-ns nmsp)))
  (.complete js/__karma__ (clj->js {})))
