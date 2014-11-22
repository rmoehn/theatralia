(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh]]
            [theatralia.main :as main]))

;;; Credits:
;;;  - https://github.com/stuartsierra/component
;;;  - http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded

(def system nil)

(defn init []
  (alter-var-root #'system
    (constantly (main/make-system {:port 17777}))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
    (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
