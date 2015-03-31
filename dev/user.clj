(ns user
  "This is the place where you start your REPLing. Anything defined or required
  in this namespace will be available at the REPL from startup. Therefore the
  functions for starting and stopping the system are situated here.

  While experimenting I usually also define various things at the bottom of this
  file, but don't check them into version control."
  (:require [com.stuartsierra.component :as component]
            [clojure.repl :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [theatralia.main :as main]))

;;; Credits:
;;;  - https://github.com/stuartsierra/component
;;;  - http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded

(def system nil)

(defn init []
  (alter-var-root
    #'system
    (constantly (main/make-system
                  {:web-server-port 17777
                   :database-uri "datomic:dev://localhost:4334/theatralia"}))))

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
